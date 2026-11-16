
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.auth.dto.response.SocialPrepareResponse;
import com.digniche.muntum.auth.dto.response.TokenResponse;
import com.digniche.muntum.auth.social.*;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialPrepareTest {

    @Mock SocialTokenVerifier verifier;
    @Mock SocialAccountRepository socialAccountRepository;
    @Mock UserRepository userRepository;
    @Mock SignupConsentRecorder signupConsentRecorder;
    @Mock TokenIssuer tokenIssuer;
    @Mock AppleTokenClient appleTokenClient;
    @Mock SocialTokenCipher socialTokenCipher;
    @Mock PlatformTransactionManager transactionManager;
    @Mock PendingSocialSignupStore ticketStore;

    private SocialLoginService service;

    @BeforeEach
    void setUp() {
        service = new SocialLoginService(
                List.of(verifier),
                socialAccountRepository,
                userRepository,
                signupConsentRecorder,
                tokenIssuer,
                appleTokenClient,
                socialTokenCipher,
                transactionManager,
                ticketStore
        );

        // Simulate the transaction manager without MySQL.
        when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void newKakaoUserReceivesTicketWithoutAccountCreation() {
        SocialLoginRequest request = new SocialLoginRequest(
                SocialProvider.KAKAO,
                "fake-kakao-token",
                null,
                null
        );

        SocialUserInfo verifiedUser = new SocialUserInfo(
                SocialProvider.KAKAO,
                "verified-kakao-id",
                "new@example.com",
                true
        );

        when(verifier.supports()).thenReturn(SocialProvider.KAKAO);
        when(verifier.verify(request)).thenReturn(verifiedUser);

        when(socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                "verified-kakao-id"
        )).thenReturn(Optional.empty());

        when(ticketStore.issue(verifiedUser, null))
                .thenReturn(
                        new PendingSocialSignupStore.IssuedTicket(
                                "test-ticket",
                                600
                        )
                );

        SocialPrepareResponse response = service.prepare(request);

        assertEquals(
                SocialPrepareResponse.Status.SIGNUP_REQUIRED,
                response.status()
        );
        assertEquals("test-ticket", response.signupTicket());
        assertNull(response.authentication());

        verify(ticketStore).issue(verifiedUser, null);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(signupConsentRecorder);
        verify(tokenIssuer, never()).issue(any(User.class));
        verifyNoInteractions(appleTokenClient);
    }

    @Test
    void existingKakaoUserLogsInWithoutSignupTicket() {
        SocialLoginRequest request = new SocialLoginRequest(
                SocialProvider.KAKAO,
                "fake-kakao-token",
                null,
                null
        );

        SocialUserInfo verifiedUser = new SocialUserInfo(
                SocialProvider.KAKAO,
                "existing-kakao-id",
                "existing@example.com",
                true
        );

        User user = User.createSocialUser(
                "existing@example.com",
                true
        );

        SocialAccount account = SocialAccount.builder()
                .user(user)
                .provider(SocialProvider.KAKAO)
                .providerUserId("existing-kakao-id")
                .providerEmail("existing@example.com")
                .build();

        when(verifier.supports()).thenReturn(SocialProvider.KAKAO);
        when(verifier.verify(request)).thenReturn(verifiedUser);

        when(socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                "existing-kakao-id"
        )).thenReturn(Optional.of(account));

        when(tokenIssuer.issue(user)).thenReturn(
                TokenResponse.of(
                        "test-access",
                        3600000,
                        "test-refresh",
                        604800000
                )
        );

        SocialPrepareResponse response = service.prepare(request);

        assertEquals(
                SocialPrepareResponse.Status.LOGIN_COMPLETE,
                response.status()
        );

        assertEquals(
                "test-access",
                response.authentication().accessToken()
        );

        assertNull(response.signupTicket());

        verifyNoInteractions(ticketStore);
        verify(userRepository, never()).save(any(User.class));
    }
}
