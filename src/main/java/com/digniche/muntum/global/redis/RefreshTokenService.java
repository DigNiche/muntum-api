package com.digniche.muntum.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "refresh:";

    // 저장: 로그인 / 토큰 재발급 시
    public void save(UUID userId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue()
                .set(PREFIX + userId, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    // 조회: 재발급 요청 시 Redis에 저장된 토큰과 비교 : null인 경우 Redis에 토큰 없는=로그아웃 또는 만료된 상태
    public String get(UUID userId) {
        return redisTemplate.opsForValue().get(PREFIX + userId);
    }

    // 삭제: 로그아웃 / Token Rotation 시
    public void delete(UUID userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}


/* 검증 방법
로그인 후
redis-cli GET refresh:{userId}
로 저장 여부 직접 확인
RedisTokenService는 다음 단계인 AuthService.login() 수정 시 실제로 연결되어 동작 검증 가능

 */