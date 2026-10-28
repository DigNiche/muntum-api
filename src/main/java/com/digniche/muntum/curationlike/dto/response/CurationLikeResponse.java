package com.digniche.muntum.curationlike.dto.response;

/**
 * 큐레이션 좋아요 수와 내 상태
 */
public record CurationLikeResponse(
        int count,
        boolean liked // 비로그인 사용자의 경우, false
) {
}
