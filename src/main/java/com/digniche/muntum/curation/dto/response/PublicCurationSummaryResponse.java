package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 공개 큐레이션 요약(카드 또는 목록) 응답
 */
public record PublicCurationSummaryResponse(
        UUID id,
        CuratorProfileResponse curator,
        String tagline,
        String thumbnailUrl
) {
    public static PublicCurationSummaryResponse from(
            Curation curation,
            CuratorProfileResponse curator,
            String thumbnailUrl
    ) {
        return new PublicCurationSummaryResponse(
                curation.getId(),
                curator,
                curation.getTagline(),
                thumbnailUrl
        );
    }
}
