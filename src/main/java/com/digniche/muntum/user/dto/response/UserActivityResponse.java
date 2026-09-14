package com.digniche.muntum.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 사용자 활동 정보 응답 DTO
 * - 관람객 : 스크랩한 수, 제보글 작성 수, 취향 키워드 개수,
 * - 큐레이터 : + 큐레이션 작성글 수 등
 */
public record UserActivityResponse (
    long keywordCount,
    long suggestionCount,
    long scrapCount,
    @JsonInclude(JsonInclude.Include.NON_NULL) long curationCount
) {
    public static UserActivityResponse from(long keywordCount, long suggestionsCount, long scrapCount, long curationCount) {
        return new UserActivityResponse(keywordCount, suggestionsCount, scrapCount, curationCount);
    }
}