package com.digniche.muntum.curationlike.dto.response;

import com.digniche.muntum.curation.entity.Curation;

/**
 * 큐레이션 좋아요 수와 내 상태
 */
public record CurationLikeResponse(
        int count,
        boolean liked
) {
    public static CurationLikeResponse of(Curation curation, boolean liked) {
        return new CurationLikeResponse(curation.getLikeCount(), liked);
    }
}
