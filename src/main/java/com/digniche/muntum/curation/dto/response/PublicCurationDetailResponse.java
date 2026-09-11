package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PublicCurationDetailResponse(
        UUID id,
        UUID programId,
        CuratorProfileResponse curator,
        String tagline,
        String content,
        List<CurationImageResponse> images
) {
    public static PublicCurationDetailResponse from(
            Curation curation,
            CuratorProfileResponse curator,
            List<CurationImageResponse> images
    ) {
        return new PublicCurationDetailResponse(
                curation.getId(),
                curation.getProgram().getId(),
                curator,
                curation.getTagline(),
                curation.getContent(),
                images
        );
    }
}
