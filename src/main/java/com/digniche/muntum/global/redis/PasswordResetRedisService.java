package com.digniche.muntum.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 비밀번호 재설정 인증번호 / 재설정 토큰 Redis 저장소
 */
@Service
@RequiredArgsConstructor
public class PasswordResetRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String CODE_PREFIX = "pwreset:code:";
    private static final String ATTEMPT_PREFIX = "pwreset:attempt:";
    private static final String TOKEN_PREFIX = "pwreset:token:";

    // 인증번호 저장 (기존 값 및 시도 횟수 초기화)
    public void saveCode(String email, String code, Duration ttl) {
        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, ttl);
        redisTemplate.delete(ATTEMPT_PREFIX + email);
    }

    public String getCode(String email) {
        return redisTemplate.opsForValue().get(CODE_PREFIX + email);
    }

    public void deleteCode(String email) {
        redisTemplate.delete(CODE_PREFIX + email);
    }

    // 인증 시도 횟수 증가 (코드와 동일한 TTL 유지)
    public long incrementAttempt(String email, Duration ttl) {
        String key = ATTEMPT_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return attempts == null ? 0 : attempts;
    }

    public void deleteAttempt(String email) {
        redisTemplate.delete(ATTEMPT_PREFIX + email);
    }

    // 인증 성공 후 발급하는 1회용 재설정 토큰
    public void saveResetToken(String resetToken, String email, Duration ttl) {
        redisTemplate.opsForValue().set(TOKEN_PREFIX + resetToken, email, ttl);
    }

    public String getEmailByResetToken(String resetToken) {
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + resetToken);
    }

    public void deleteResetToken(String resetToken) {
        redisTemplate.delete(TOKEN_PREFIX + resetToken);
    }
}
