package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.social.SocialTokenVerifier;
import com.digniche.muntum.auth.social.SocialUserInfo;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.SocialAccount;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.SocialAccountRepository;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.digniche.muntum.auth.social.AppleTokenClient;
import com.digniche.muntum.auth.social.AppleTokenResponse;
import com.digniche.muntum.auth.social.SocialTokenCipher;
import com.digniche.muntum.user.entity.SocialProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.digniche.muntum.auth.dto.response.SocialPrepareResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final List<SocialTokenVerifier> verifiers;

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final SignupConsentRecorder signupConsentRecorder;

    private final TokenIssuer tokenIssuer;

    private final AppleTokenClient appleTokenClient;
    private final SocialTokenCipher socialTokenCipher;
    private final PlatformTransactionManager transactionManager;
    private final PendingSocialSignupStore pendingSocialSignupStore;

    public AuthenticationResponse login(
            SocialLoginRequest request
    ) {
        /*
         * 요청 provider에 맞는 검증기 선택
         */
        SocialTokenVerifier verifier = findVerifier(request.provider());

        /*
         * 앱이 전달한 Identity Token 검증
         */
        SocialUserInfo socialUser = verifier.verify(request);

        /*
         * Apple authorizationCode 교환 및
         * refresh token 암호화
         *
         * 외부 Apple 서버 호출이므로
         * DB 트랜잭션 시작 전에 실행
         */
        String encryptedProviderRefreshToken =
                prepareProviderRefreshToken(request, verifier, socialUser);

        /*
         * 아래 DB 작업만 트랜잭션으로 실행
         */
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        User committedUser = transactionTemplate.execute(status->
                completeLogin(socialUser, encryptedProviderRefreshToken));

        if (committedUser == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
        return AuthenticationResponse.of(
                tokenIssuer.issue(committedUser),
                committedUser
        );
    }

    /**
     * New two-phase social authentication entry point.
     *
     * Existing user:
     *   complete login and return Muntum JWTs.
     *
     * New user:
     *   issue a temporary signup ticket.
     *   Do NOT create a Muntum User.
     */

    public SocialPrepareResponse prepare(SocialLoginRequest request) {
        SocialTokenVerifier verifier = findVerifier(request.provider());
        SocialUserInfo socialUser = verifier.verify(request);

        String encryptedProviderRefreshToken =
                prepareProviderRefreshToken(request, verifier, socialUser);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        User existingUser = transactionTemplate.execute(status ->
                socialAccountRepository
                        .findByProviderAndProviderUserId(
                                socialUser.provider(),
                                socialUser.providerUserId()
                        )
                        .map(socialAccount ->
                                prepareAuthenticatedUser(
                                        socialAccount,
                                        encryptedProviderRefreshToken
                                )
                        )
                        .orElse(null)
        );

        // 기존 회원: DB 커밋 이후 JWT 발급
        if (existingUser != null) {
            AuthenticationResponse authentication =
                    AuthenticationResponse.of(
                            tokenIssuer.issue(existingUser),
                            existingUser
                    );

            return SocialPrepareResponse.loginComplete(authentication);
        }

        // 신규 회원: 계정 생성 없이 가입 티켓 발급
        PendingSocialSignupStore.IssuedTicket issuedTicket =
                pendingSocialSignupStore.issue(
                        socialUser,
                        encryptedProviderRefreshToken
                );

        return SocialPrepareResponse.signupRequired(
                issuedTicket.ticket(),
                issuedTicket.expiresIn()
        );
    }


    private String prepareProviderRefreshToken(
            SocialLoginRequest request,
            SocialTokenVerifier verifier,
            SocialUserInfo socialUser
    ) {
        /*
         * 카카오는 추후 별도 처리
         */
        if (request.provider() != SocialProvider.APPLE) {
            return null;
        }

        /*
         * authorizationCode를 Apple 서버에 보내
         * access token, refresh token, id token 수신
         */
        AppleTokenResponse tokenResponse =
                appleTokenClient.exchangeAuthorizationCode(
                        request.authorizationCode()
                );

        if (tokenResponse.idToken() == null
                || tokenResponse.idToken().isBlank()
                || tokenResponse.refreshToken() == null
                || tokenResponse.refreshToken().isBlank()) {
            throw new BusinessException(ErrorCode.APPLE_TOKEN_EXCHANGE_FAILED);
        }

        /*
         * authorizationCode 교환 결과의 id_token도 검증
         *
         * 앱이 보낸 Identity Token의 사용자와
         * authorizationCode의 사용자가 동일한지 확인
         */
        SocialLoginRequest exchangedTokenRequest =
                new SocialLoginRequest(
                        SocialProvider.APPLE,
                        tokenResponse.idToken(),
                        null,
                        request.nonce()
                );

        SocialUserInfo exchangedUser =
                verifier.verify(exchangedTokenRequest);
        // 프론트엔드가 줬던 신분증의 유저(socialUser) == 애플이 방금 준 신분증의 유저(exchangedUser) 비교
        if (!socialUser.providerUserId().equals(
                exchangedUser.providerUserId()
        )) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        /*
         * DB에는 refresh token 평문이 아닌
         * AES-GCM 암호문 저장
         */
        return socialTokenCipher.encrypt(
                tokenResponse.refreshToken()
        );
    }


    private User completeLogin(
            SocialUserInfo socialUser,
            String encryptedProviderRefreshToken
    ) {
        SocialAccount socialAccount =
                socialAccountRepository
                        .findByProviderAndProviderUserId(
                                socialUser.provider(),
                                socialUser.providerUserId()
                        )
                        .orElseGet(() ->
                                createSocialAccount(socialUser)
                        );

        return prepareAuthenticatedUser(
                socialAccount,
                encryptedProviderRefreshToken
        );
    }


    private User prepareAuthenticatedUser(
            SocialAccount socialAccount,
            String encryptedProviderRefreshToken
    ) {
        User user = socialAccount.getUser();

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }

        if (encryptedProviderRefreshToken != null) {
            socialAccount.updateProviderRefreshToken(
                    encryptedProviderRefreshToken
            );
        }

        user.updateLastLogin();

        return user;
    }

    /**
     * 구버전 앱과의 임시 회원가입 호환성 유지.
     *
     * 기존 계정은 completeLogin()에서 처리한다.
     * 처음 가입하는 Apple 사용자만 이 메서드로 들어와
     * 기존 API를 통해 계정을 생성할 수 있다.
     */
    private SocialAccount createSocialAccount(
            SocialUserInfo socialUser
    ) {

        // 1. 신규 Kakao 사용자는 명시적 동의 절차를 따라야 한다.
        if (socialUser.provider() != SocialProvider.APPLE) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_SIGNUP_REQUIRED
            );
        }

        // 2. 기존 이메일 필수 정책 유지
        String email = socialUser.email();

        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_EMAIL_REQUIRED
            );
        }

        email = email.trim();

        // 3. 이메일을 기준으로 계정을 자동 연결하지 않는 정책 유지
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                    ErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        // 4. 구버전 Apple 사용자 생성
        User user = User.createSocialUser(
                email,
                socialUser.emailVerified()
        );

        userRepository.save(user);

        // 5. 실제 확인되지 않은 동의를 동의한 것으로 기록하지 않음
        // 이 메서드는 기존 DB 트랜잭션 안에서 실행되어야 함
        signupConsentRecorder.saveLegacyUnverifiedSignup(user);

        // 6. 기존 방식과 동일하게 소셜 계정 생성
        SocialAccount socialAccount =
                SocialAccount.builder()
                        .user(user)
                        .provider(socialUser.provider())
                        .providerUserId(
                                socialUser.providerUserId()
                        )
                        .providerEmail(email)
                        .build();

        return socialAccountRepository.save(socialAccount);
    }

    private SocialTokenVerifier findVerifier(
            SocialProvider provider
    ) {
        return verifiers.stream()
                .filter(v -> v.supports() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER)
                );
    }
}