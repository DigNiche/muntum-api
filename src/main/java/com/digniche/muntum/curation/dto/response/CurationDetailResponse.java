package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curation.entity.CurationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CurationDetailResponse(
        UUID id,
        UUID programId,
        String submittedProgramTitle,
        String submittedPlace,
        String tagline,
        String content,
        List<CurationImageResponse> images,
        CurationStatus status,
        CurationPublicationStatus publicationStatus,
        String changeRequestReason,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CurationDetailResponse from(
            Curation curation,
            List<CurationImageResponse> images
    ) {
        return new CurationDetailResponse(
                curation.getId(),
                curation.getProgram() != null
                        ? curation.getProgram().getId()
                        : null,
                curation.getSubmittedProgramTitle(),
                curation.getSubmittedPlace(),
                curation.getTagline(),
                curation.getContent(),
                images,
                curation.getStatus(),
                curation.getPublicationStatus(),
                curation.getChangeRequestReason(),
                curation.getReviewedAt(),
                curation.getCreatedAt(),
                curation.getUpdatedAt()
        );
    }
}