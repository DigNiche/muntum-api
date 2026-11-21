package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.global.auditor.UserDisplay;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ManagerCurationDetailResponse(
        UUID id,
        UUID programId,
        CuratorProfileResponse curator,
        String submittedProgramTitle,
        String submittedPlace,
        String tagline,
        String content,
        List<CurationImageResponse> images,
        CurationStatus status,
        CurationPublicationStatus publicationStatus,
        CurationLikeResponse like,
        String changeRequestReason,
        UUID reviewerId,
        String reviewerNickname,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ManagerCurationDetailResponse from(Curation curation, CuratorProfileResponse curator, List<CurationImageResponse> images, CurationLikeResponse like, UserDisplay reviewer) {
        return new ManagerCurationDetailResponse(
                curation.getId(),
                curation.getProgram() != null
                        ? curation.getProgram().getId()
                        : null,
                curator,
                curation.getSubmittedProgramTitle(),
                curation.getSubmittedPlace(),
                curation.getTagline(),
                curation.getContent(),
                images,
                curation.getStatus(),
                curation.getPublicationStatus(),
                like,
                curation.getChangeRequestReason(),
                reviewer != null ? reviewer.id() : null,
                reviewer != null ? reviewer.nickname() : null,
                curation.getReviewedAt(),
                curation.getCreatedAt(),
                curation.getUpdatedAt()
        );
    }
}