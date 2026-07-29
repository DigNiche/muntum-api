package com.digniche.muntum.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailVerificationRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String CODE_PREFIX = "emailverify:code:";         // 코드 대조(TTL: 5 min)
    private static final String ATTEMPT_PREFIX = "emailverify:attempt:";   // 5회 초과 시 차단(TTL: 5 min)
    private static final String COOLDOWN_PREFIX = "emailverify:cooldown:"; // 재발송 쿨다운(TTL: 60 sec)
    private static final String DAILY_PREFIX = "emailverify:daily:";       // 일일 5회 상한(TTL: 24 hour)
    private static final String TOKEN_PREFIX = "signup:token:";            // 가입 시 1회용 인증 증표(TTL: 30 min)


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

    // 재발송 쿨다운
    public boolean isInCooldown(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_PREFIX + email));
    }

    public void setCooldown(String email, Duration ttl) {
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1", ttl);
    }

    // 일일 발송 횟수 증가 (최초 발송 시에만 TTL 설정)
    public long incrementDailyCount(String email, Duration ttl) {
        String key = DAILY_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return count == null ? 0 : count;
    }

    // 인증 성공 후 발급하는 1회용 가입 토큰
    public void saveSignupToken(String signupToken, String email, Duration ttl) {
        redisTemplate.opsForValue().set(TOKEN_PREFIX + signupToken, email, ttl);
    }

    public String getEmailBySignupToken(String signupToken) {
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + signupToken);
    }

    public void deleteSignupToken(String signupToken) {
        redisTemplate.delete(TOKEN_PREFIX + signupToken);
    }
}