
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SocialSignupRequest;
import com.digniche.muntum.auth.dto.request.SignupConsentRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.SocialAccountRepository;
import com.digniche.muntum.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialSignupService {

    private final PendingSocialSignupStore pendingSocialSignupStore;
    private final SignupConsentRecorder signupConsentRecorder;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenIssuer tokenIssuer;
    private final PlatformTransactionManager transactionManager;

    /**
     * Complete signup using an identity verified during /social/prepare.
     *
     * No Apple/Kakao API calls occur here.
     */
    public AuthenticationResponse signup(SocialSignupRequest request) {

        // 1. Reserve the ticket and retrieve the verified identity.
        PendingSocialSignupStore.ReservedTicket reserved =
                pendingSocialSignupStore.reserve(request.signupTicket());

        User committedUser;

        // 2. Create account and consent in one MySQL transaction.
        try {
            TransactionTemplate transactionTemplate =
                    new TransactionTemplate(transactionManager);

            committedUser = transactionTemplate.execute(status ->
                    createAccount(reserved.pending(), request.signupConsent())
            );

            if (committedUser == null) {
                throw new IllegalStateException("Social signup transaction returned no user");
            }

        } catch (DataIntegrityViolationException exception) {

            // Database unique constraints are the final defense
            // against simultaneous duplicate registrations.
            releaseSafely(reserved, exception);

            throw new BusinessException(ErrorCode.SOCIAL_SIGNUP_CONFLICT);

        } catch (RuntimeException exception) {

            // The transaction failed. Retain the ticket for
            // a possible corrected submission or retry.
            releaseSafely(reserved, exception);

            throw exception;
        }

        // 3. MySQL has committed at this point.
        // Never release the ticket after this boundary.
        boolean completed;

        try {
            completed = pendingSocialSignupStore.complete(reserved);

        } catch (RuntimeException exception) {
            log.error("Social signup committed but ticket completion failed", exception);

            throw new BusinessException(ErrorCode.SOCIAL_SIGNUP_CONFIRMATION_FAILED);
        }

        if (!completed) {
            // The lease may have expired or changed ownership.
            // Require fresh provider authentication.
            throw new BusinessException(ErrorCode.SOCIAL_SIGNUP_CONFIRMATION_FAILED);
        }

        // 4. Issue Muntum tokens only after successful commit
        // and ticket completion.
        return AuthenticationResponse.of(tokenIssuer.issue(committedUser), committedUser);
    }

    /**
     * Runs inside the TransactionTemplate.
     * No external provider or Redis calls.
     */
    private User createAccount(PendingSocialSignup pending, SignupConsentRequest consent) {

        // 1. Never use an unverified provider ID from Flutter.
        // The identity comes exclusively from the Redis ticket.
        if (socialAccountRepository
                .findByProviderAndProviderUserId(pending.provider(), pending.providerUserId())
                .isPresent()) {

            throw new BusinessException(ErrorCode.SOCIAL_SIGNUP_CONFLICT);
        }

        String email = pending.email().trim();

        // 2. Preserve the existing no-auto-linking email policy.
        if (userRepository.existsByEmailAndStatusNot(email, UserStatus.DELETED)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. Create the new Muntum user.
        User user = User.createSocialUser(email, pending.emailVerified());

        user.updateLastLogin();
        userRepository.save(user);

        // 4. Validate and persist actual signup choices.
        // This method requires an existing DB transaction.
        signupConsentRecorder.saveExplicitSignup(user, consent);

        // 5. Link the verified provider identity.
        SocialAccount socialAccount =
                SocialAccount.builder()
                        .user(user)
                        .provider(pending.provider())
                        .providerUserId(pending.providerUserId())
                        .providerEmail(email)
                        .providerRefreshToken(pending.encryptedAppleRefreshToken())
                        .build();

        // 6. Force pending database writes to execute.
        // The transaction still commits after this method returns.
        socialAccountRepository.saveAndFlush(socialAccount);

        return user;
    }

    /**
     * Best-effort ticket release after a failed DB transaction.
     * Never replace the original failure with a Redis cleanup error.
     */
    private void releaseSafely(
            PendingSocialSignupStore.ReservedTicket reserved,
            RuntimeException originalFailure
    ) {
        try {
            boolean released = pendingSocialSignupStore.release(reserved);

            if (!released) {
                log.warn("Could not release social signup lease");
            }

        } catch (RuntimeException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);

            log.warn("Social signup lease cleanup failed");
        }
    }
}
