package com.digniche.muntum.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 최근 검색어 서비스 (Redis Sorted Set)
 * - member = 검색어, score = 검색 시각(ms)
 * - 로그인 유저만 서버 저장 (게스트는 프론트 로컬)
 */
@Service
@RequiredArgsConstructor
public class RecentSearchService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "recent_search:";
    private static final int MAX_SIZE = 10;
    private static final Duration TTL = Duration.ofDays(30);

    private String key(UUID userId) {
        return PREFIX + userId;
    }

    // 저장: 있으면 시각 갱신(중복 X), 10개 초과분 제거, TTL 갱신
    public void save(UUID userId, String query) {
        String key = key(userId);
        double score = System.currentTimeMillis();

        redisTemplate.opsForZSet().add(key, query, score);

        // 최신 MAX_SIZE개만 남기고 오래된 것 제거
        // ZSet은 score 오름차순(0=가장 오래됨). 뒤에서 MAX_SIZE개를 남기려면
        // 인덱스 0 ~ -(MAX_SIZE+1) 범위(=오래된 초과분)를 삭제.
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_SIZE + 1));

        // 검색할 때마다 TTL 갱신 (마지막 검색 기준 30일 슬라이딩)
        redisTemplate.expire(key, TTL);
    }

    // 조회: 최근순(score 내림차순) 10개
    public List<String> getRecent(UUID userId) {
        Set<String> result = redisTemplate.opsForZSet()
                .reverseRange(key(userId), 0, MAX_SIZE - 1);
        return result == null ? List.of() : List.copyOf(result);
    }

    // 개별 삭제
    public void delete(UUID userId, String query) {
        redisTemplate.opsForZSet().remove(key(userId), query);
    }

    // 전체 삭제
    public void deleteAll(UUID userId) {
        redisTemplate.delete(key(userId));
    }
}