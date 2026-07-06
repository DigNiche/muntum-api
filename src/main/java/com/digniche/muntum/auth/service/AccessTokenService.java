package com.digniche.muntum.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 탈퇴 직후 권한 사용 방지 서비스
 */
@Service
@RequiredArgsConstructor
public class AccessTokenService {
    private static final String PREFIX = "withdrawallist:";
    private final RedisTemplate<String, String> redisTemplate;

    public void addToWithdrawlList(String token, long remainingMillis) {
        redisTemplate.opsForValue().set(PREFIX + token, "1", remainingMillis, TimeUnit.MILLISECONDS);
    }
    public boolean isWithdrawn(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
