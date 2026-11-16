
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.social.SocialUserInfo;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.SocialProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingSocialSignupStoreTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @Test
    void issuedTicketIsRandomHashedAndExpiring()
            throws Exception {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                eq(Duration.ofMinutes(10))
        )).thenReturn(true);

        PendingSocialSignupStore store =
                new PendingSocialSignupStore(
                        redisTemplate,
                        new ObjectMapper()
                );

        SocialUserInfo verifiedUser = new SocialUserInfo(
                SocialProvider.KAKAO,
                "123456789",
                "test@example.com",
                true
        );

        var first = store.issue(verifiedUser, null);
        var second = store.issue(verifiedUser, null);

        assertEquals(43, first.ticket().length());
        assertEquals(600, first.expiresIn());

        assertNotEquals(
                first.ticket(),
                second.ticket()
        );

        ArgumentCaptor<String> keys =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> values =
                ArgumentCaptor.forClass(String.class);

        verify(valueOperations, times(2))
                .setIfAbsent(
                        keys.capture(),
                        values.capture(),
                        eq(Duration.ofMinutes(10))
                );

        String firstKey = keys.getAllValues().get(0);

        assertTrue(
                firstKey.startsWith("social:signup:pending:")
        );

        assertFalse(
                firstKey.contains(first.ticket()),
                "Raw ticket must not appear in Redis key"
        );

        // SHA-256 digest occupies 64 hexadecimal characters.
        assertEquals(
                64,
                firstKey.substring(
                        "social:signup:pending:".length()
                ).length()
        );

        PendingSocialSignup stored =
                new ObjectMapper().readValue(
                        values.getAllValues().get(0),
                        PendingSocialSignup.class
                );

        assertEquals(
                "123456789",
                stored.providerUserId()
        );

        assertEquals(
                SocialProvider.KAKAO,
                stored.provider()
        );

        assertNull(stored.encryptedAppleRefreshToken());
    }

    @Test
    void malformedTicketIsRejectedBeforeRedisAccess() {

        PendingSocialSignupStore store =
                new PendingSocialSignupStore(
                        redisTemplate,
                        new ObjectMapper()
                );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> store.reserve("invalid-ticket")
        );

        assertEquals(
                ErrorCode.INVALID_SOCIAL_SIGNUP_TICKET,
                exception.getErrorCode()
        );

        verifyNoInteractions(redisTemplate);
    }
}
