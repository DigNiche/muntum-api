package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CurationListResponse(
        UUID id,
        UUID programId,
        String submittedProgramTitle,
        String submittedPlace,
        String tagline,
        CurationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {
    public static CurationListResponse from(
            Curation curation
    ) {
        return new CurationListResponse(
                curation.getId(),
                curation.getProgram() != null
                        ? curation.getProgram().getId()
                        : null,
                curation.getSubmittedProgramTitle(),
                curation.getSubmittedPlace(),
                curation.getTagline(),
                curation.getStatus(),
                curation.getCreatedAt(),
                curation.getUpdatedAt(),
                curation.getPublishedAt()
        );
    }
}