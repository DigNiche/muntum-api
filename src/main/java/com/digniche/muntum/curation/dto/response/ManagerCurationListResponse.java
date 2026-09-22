package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.Curation;
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
        LocalDateTime createdAt
) {
    public static ManagerCurationListResponse from(
            Curation curation,
            CuratorProfileResponse curator
    ) {
        return new ManagerCurationListResponse(
                curation.getId(),
                curator,
                curation.getSubmittedProgramTitle(),
                curation.getSubmittedPlace(),
                curation.getTagline(),
                curation.getStatus(),
                curation.getCreatedAt()
        );
    }
}