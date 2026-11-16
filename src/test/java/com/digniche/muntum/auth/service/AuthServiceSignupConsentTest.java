
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.*;
import com.digniche.muntum.auth.dto.response.*;
import com.digniche.muntum.global.redis.EmailVerificationRedisService;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.global.security.jwt.JwtProvider;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignupConsentTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;
    @Mock RefreshTokenService refreshTokenService;
    @Mock TokenIssuer tokenIssuer;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock EmailVerificationRedisService emailVerificationRedisService;
    @Mock SignupConsentRecorder signupConsentRecorder;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks
    AuthService authService;

    private void prepareSignup(boolean successfulCompletion) {
        when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
        when(emailVerificationRedisService
                .getEmailBySignupToken("test-ticket"))
                .thenReturn("test@example.com");

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        // Return the user being saved without using MySQL.
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        if (successfulCompletion) {
            when(tokenIssuer.issue(any(User.class)))
                    .thenReturn(TokenResponse.of(
                            "test-access",
                            3600000L,
                            "test-refresh",
                            604800000L
                    ));
        }
    }

    @Test
    void explicitSignupUsesActualConsent() {
        prepareSignup(true);

        SignupConsentRequest consent =
                new SignupConsentRequest(
                        List.of(new SignupConsentChoice(
                                UserTermsType.TERMS_OF_SERVICE,
                                "1.0.1",
                                true
                        ))
                );

        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "test-ticket",
                "1.0.1",
                consent
        );

        authService.signup(request);

        verify(signupConsentRecorder).saveExplicitSignup(
                any(User.class),
                same(consent)
        );

        verify(signupConsentRecorder, never())
                .saveLegacyUnverifiedSignup(any(User.class));

        verify(userRepository).flush();
        verify(tokenIssuer).issue(any(User.class));
        verify(emailVerificationRedisService)
                .deleteSignupToken("test-ticket");
    }

    @Test
    void legacySignupUsesUnverifiedRecorder() {
        prepareSignup(true);

        // Existing four-argument request.
        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "test-ticket",
                "1.0.1"
        );

        authService.signup(request);

        verify(signupConsentRecorder)
                .saveLegacyUnverifiedSignup(any(User.class));

        verify(signupConsentRecorder, never())
                .saveExplicitSignup(
                        any(User.class),
                        any(SignupConsentRequest.class)
                );

        verify(tokenIssuer).issue(any(User.class));
    }

    @Test
    void invalidExplicitConsentMustNotFallBackToLegacy() {
        prepareSignup(false);

        SignupConsentRequest consent =
                new SignupConsentRequest(
                        List.of(new SignupConsentChoice(
                                UserTermsType.TERMS_OF_SERVICE,
                                "1.0.1",
                                false
                        ))
                );

        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "test-ticket",
                "1.0.1",
                consent
        );

        // Simulate the real recorder rejecting the decision.
        when(signupConsentRecorder.saveExplicitSignup(
                any(User.class),
                same(consent)
        )).thenThrow(new com.digniche.muntum.global.exception
                .BusinessException(
                com.digniche.muntum.global.exception
                        .ErrorCode.REQUIRED_TERMS_NOT_AGREED
        ));

        assertThrows(
                com.digniche.muntum.global.exception
                        .BusinessException.class,
                () -> authService.signup(request)
        );

        verify(signupConsentRecorder, never())
                .saveLegacyUnverifiedSignup(any(User.class));

        verify(tokenIssuer, never()).issue(any(User.class));

        verify(emailVerificationRedisService, never())
                .deleteSignupToken(anyString());
    }

    @Test
    void oldJsonRequestStillDeserializes() throws Exception {
        String json = """
                {
                  "email": "test@example.com",
                  "password": "password123",
                  "signupToken": "test-ticket",
                  "userTermsAgreementVersion": "1.0.1"
                }
                """;

        SignUpRequest request =
                new ObjectMapper().readValue(
                        json, SignUpRequest.class
                );

        assertNull(request.signupConsent());
        assertEquals("1.0.1",
                request.userTermsAgreementVersion());
    }

    @Test
    void commitFailureMustNotIssueJwtOrConsumeSignupToken() {

        prepareSignup(false);

        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "test-ticket",
                "1.0.1"
        );

        doThrow(new TransactionSystemException("Commit failed"))
                .when(transactionManager)
                .commit(any(TransactionStatus.class));

        assertThrows(
                TransactionSystemException.class,
                () -> authService.signup(request)
        );

        verify(tokenIssuer, never()).issue(any(User.class));

        verify(emailVerificationRedisService, never())
                .deleteSignupToken(anyString());
    }
}
