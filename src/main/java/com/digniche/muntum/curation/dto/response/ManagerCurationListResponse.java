package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record ManagerCurationListResponse(
        UUID id,
        CuratorProfileResponse curator,
        String submittedProgramTitle,
        String submittedPlace,
        String tagline,
        CurationStatus status,
        CurationLikeResponse like,
        LocalDateTime createdAt
) {
    public static ManagerCurationListResponse from(Curation curation, CuratorProfileResponse curator, CurationLikeResponse like) {
        return new ManagerCurationListResponse(
                curation.getId(),
                curator,
                curation.getSubmittedProgramTitle(),
                curation.getSubmittedPlace(),
                curation.getTagline(),
                curation.getStatus(),
                like,
                curation.getCreatedAt()
        );
    }
}