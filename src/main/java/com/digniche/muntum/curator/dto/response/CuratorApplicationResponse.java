package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 큐레이터 지원서 상세 조회 응답 DTO
 */
public record CuratorApplicationResponse(
        UUID id,
        ApplicantProfileResponse applicant,
        String programName,
        String tagline,
        String curation,
        ApplicantStatusInfoResponse statusInfo,
        ReviewerProfileResponse reviewer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CuratorApplicationResponse from(CuratorApplication application, ReviewerProfileResponse reviewer) {
        return new CuratorApplicationResponse(
                application.getId(),
                ApplicantProfileResponse.from(application.getApplicant()),
                application.getProgramName(),
                application.getTagline(),
                application.getCuration(),
                ApplicantStatusInfoResponse.from(application),reviewer,
                application.getCreatedAt(),
                application.getUpdatedAt()

        );
    }
}