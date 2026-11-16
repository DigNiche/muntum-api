
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.*;
import com.digniche.muntum.auth.dto.response.TokenResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialSignupServiceTest {

    @Mock PendingSocialSignupStore ticketStore;
    @Mock SignupConsentRecorder consentRecorder;
    @Mock UserRepository userRepository;
    @Mock SocialAccountRepository socialAccountRepository;
    @Mock TokenIssuer tokenIssuer;
    @Mock PlatformTransactionManager transactionManager;

    private SocialSignupService service;

    @BeforeEach
    void setUp() {
        service = new SocialSignupService(
                ticketStore,
                consentRecorder,
                userRepository,
                socialAccountRepository,
                tokenIssuer,
                transactionManager
        );

        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)
        )).thenReturn(new SimpleTransactionStatus());
    }

    private PendingSocialSignupStore.ReservedTicket reservation() {
        PendingSocialSignup pending =
                new PendingSocialSignup(
                        SocialProvider.KAKAO,
                        "verified-kakao-id",
                        "new@example.test",
                        true,
                        null
                );

        return new PendingSocialSignupStore.ReservedTicket(
                "ticket-digest",
                "lease-id",
                pending
        );
    }

    private SocialSignupRequest signupRequest() {
        return new SocialSignupRequest(
                "test-ticket",
                new SignupConsentRequest(List.of(
                        new SignupConsentChoice(
                                UserTermsType.TERMS_OF_SERVICE,
                                "1.0.1",
                                true
                        )
                ))
        );
    }

    @Test
    void commitsBeforeTicketCompletionAndJwtIssuance() {

        var reserved = reservation();

        when(ticketStore.reserve("test-ticket"))
                .thenReturn(reserved);

        when(socialAccountRepository
                .findByProviderAndProviderUserId(
                        SocialProvider.KAKAO,
                        "verified-kakao-id"
                ))
                .thenReturn(Optional.empty());

        when(ticketStore.complete(reserved)).thenReturn(true);

        when(tokenIssuer.issue(any(User.class)))
                .thenReturn(TokenResponse.of(
                        "access-token",
                        3600000L,
                        "refresh-token",
                        604800000L
                ));

        var response = service.signup(signupRequest());

        assertEquals("access-token", response.accessToken());

        InOrder order = inOrder(
                transactionManager,
                ticketStore,
                tokenIssuer
        );

        order.verify(transactionManager)
                .commit(any(TransactionStatus.class));

        order.verify(ticketStore).complete(reserved);

        order.verify(tokenIssuer).issue(any(User.class));

        verify(consentRecorder).saveExplicitSignup(
                any(User.class),
                any(SignupConsentRequest.class)
        );
    }

    @Test
    void rejectedConsentReleasesTicketWithoutIssuingJwt() {

        var reserved = reservation();
        var request = signupRequest();

        when(ticketStore.reserve("test-ticket"))
                .thenReturn(reserved);

        when(socialAccountRepository
                .findByProviderAndProviderUserId(
                        SocialProvider.KAKAO,
                        "verified-kakao-id"
                ))
                .thenReturn(Optional.empty());

        doThrow(new BusinessException(
                ErrorCode.REQUIRED_TERMS_NOT_AGREED
        )).when(consentRecorder).saveExplicitSignup(
                any(User.class),
                same(request.signupConsent())
        );

        assertThrows(
                BusinessException.class,
                () -> service.signup(request)
        );

        verify(transactionManager)
                .rollback(any(TransactionStatus.class));

        verify(ticketStore).release(reserved);
        verify(ticketStore, never()).complete(any());

        verify(tokenIssuer, never()).issue(any(User.class));
    }
}
