
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.social.SocialUserInfo;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PendingSocialSignupStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Duration TICKET_TTL = Duration.ofMinutes(10); //가입 대기 정보가 유효한 기간
    private static final Duration LEASE_TTL = Duration.ofMinutes(2); //특정 요청이 티켓의 처리 권한을 독점하는 최대 시간

    private static final String PENDING_PREFIX = "social:signup:pending:";
    private static final String LEASE_PREFIX = "social:signup:lease:";

    // 32 random bytes encoded with Base64 URL, without padding.
    private static final Pattern TICKET_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    /*
     * Atomically:
     * 1. Read the pending signup.
     * 2. Create an exclusive processing lease.
     * 3. Return the pending signup only to the lease owner.
     *
     * null = missing or expired ticket
     * ""   = another request owns the lease
     */
    private static final RedisScript<String> CLAIM_SCRIPT =
            stringScript("""
                    local data = redis.call('GET', KEYS[1])

                    if not data then
                        return nil
                    end

                    local acquired = redis.call(
                        'SET', KEYS[2], ARGV[1],
                        'NX', 'PX', ARGV[2]
                    )

                    if not acquired then
                        return ''
                    end

                    return data
                    """);

    /*
     * Release only if the caller still owns the lease.
     * Keep the pending signup for a legitimate retry.
     */
    private static final RedisScript<Long> RELEASE_SCRIPT =
            longScript("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end

                    return 0
                    """);

    /*
     * After successful DB commit:
     * delete the pending identity and the owned lease.
     */
    private static final RedisScript<Long> COMPLETE_SCRIPT =
            longScript("""
                    if redis.call('GET', KEYS[2]) ~= ARGV[1] then
                        return 0
                    end

                    redis.call('DEL', KEYS[1], KEYS[2])
                    return 1
                    """);

    /**
     * 소셜 인증 후 호출
     */
    public IssuedTicket issue(
            SocialUserInfo socialUser,
            String encryptedAppleRefreshToken
    ) {
        if (socialUser == null) {
            throw new IllegalArgumentException("Verified social identity is required");
        }

        if (socialUser.email() == null
                || socialUser.email().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }

        PendingSocialSignup pending =
                new PendingSocialSignup(
                        socialUser.provider(),
                        socialUser.providerUserId(),
                        socialUser.email(),
                        socialUser.emailVerified(),
                        encryptedAppleRefreshToken
                );

        String json = serialize(pending);

        // A collision is extremely unlikely, but we never overwrite another live pending signup.
        for (int attempt = 0; attempt < 3; attempt++) {
            String ticket = newRandomToken();
            String digest = sha256Hex(ticket);

            Boolean created = redisTemplate
                    .opsForValue()
                    .setIfAbsent(pendingKey(digest), json, TICKET_TTL);

            if (Boolean.TRUE.equals(created)) {
                return new IssuedTicket(ticket, TICKET_TTL.toSeconds());
            }

            if (created == null) {
                throw new IllegalStateException("Redis ticket creation did not return a result");
            }
        }

        throw new IllegalStateException("Could not create a unique signup ticket");
    }

    /**
     * Reserve a ticket for one signup attempt.
     * Does not consume the ticket or create an account.
     */
    public ReservedTicket reserve(String rawTicket) {
        String digest = checkedDigest(rawTicket);
        String leaseId = newRandomToken();

        String json = redisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(pendingKey(digest), leaseKey(digest)),
                leaseId,
                Long.toString(LEASE_TTL.toMillis())
        );

        if (json == null) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_SIGNUP_TICKET);
        }

        if (json.isEmpty()) {
            throw new BusinessException(ErrorCode.SOCIAL_SIGNUP_IN_PROGRESS);
        }

        try {
            PendingSocialSignup pending = objectMapper.readValue(json, PendingSocialSignup.class);

            return new ReservedTicket(digest, leaseId, pending);

        } catch (JsonProcessingException
                 | IllegalArgumentException exception) {

            // Invalid server-side state: release the lease,
            // but never trust or return the corrupted data.
            releaseLease(digest, leaseId);

            throw new BusinessException(ErrorCode.INVALID_SOCIAL_SIGNUP_TICKET);
        }
    }

    /**
     * Call after rollback or a failed consent validation.
     * Returns false if ownership was already lost.
     */
    public boolean release(ReservedTicket reserved) {
        return releaseLease(reserved.ticketDigest(), reserved.leaseId());
    }

    /**
     * Call only after the signup DB transaction commits.
     * A successful call makes this ticket unusable.
     */
    public boolean complete(ReservedTicket reserved) {
        Long result = redisTemplate.execute(
                COMPLETE_SCRIPT,
                List.of(pendingKey(reserved.ticketDigest()), leaseKey(reserved.ticketDigest())),
                reserved.leaseId()
        );

        return Long.valueOf(1L).equals(result);
    }
    // 잠금 소유자 확인 후 잠금 삭제
    private boolean releaseLease(String digest, String leaseId) {
        Long result = redisTemplate.execute(RELEASE_SCRIPT, List.of(leaseKey(digest)), leaseId);

        return Long.valueOf(1L).equals(result);
    }
    // PendingSocialSignup 객체를 JSON으로 변환
    private String serialize(PendingSocialSignup pending) {
        try {
            return objectMapper.writeValueAsString(pending);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize pending signup", exception);
        }
    }
    // 티켓 형식 확인 후 SHA-256 해시 계산
    private static String checkedDigest(String rawTicket) {
        if (rawTicket == null
                || !TICKET_PATTERN.matcher(rawTicket).matches()) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_SIGNUP_TICKET);
        }

        return sha256Hex(rawTicket);
    }
    // SHA-256 해시를 16진수 문자열로 변환
    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(bytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
    // 	32바이트 보안 난수로 예측하기 어려운 티켓 생성
    private static String newRandomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    // 가입 대기 데이터용 Redis 키 생성
    private static String pendingKey(String digest) {
        return PENDING_PREFIX + digest;
    }
    // 처리 잠금용 Redis 키 생성
    private static String leaseKey(String digest) {
        return LEASE_PREFIX + digest;
    }
    // 문자열 반환 Lua 스크립트 설정
    private static RedisScript<String> stringScript(
            String source
    ) {
        DefaultRedisScript<String> script =
                new DefaultRedisScript<>();

        script.setScriptText(source);
        script.setResultType(String.class);
        return script;
    }
    // 	숫자 반환 Lua 스크립트 설정
    private static RedisScript<Long> longScript(
            String source
    ) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();

        script.setScriptText(source);
        script.setResultType(Long.class);
        return script;
    }
    //티켓 최초 발급 시 반환하는 데이터
    public record IssuedTicket(String ticket, long expiresIn) {
    }
    // 서버가 티켓 선점 후 사용하는 데이터
    public record ReservedTicket(String ticketDigest, String leaseId, PendingSocialSignup pending) {
    }
}
