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
//        String applicantProfileImageUrl,
        String programName,
        String tagline,
        String curation,
        CuratorApplicationStatus status,
        CuratorApplicationRejectReason rejectReason,
        String rejectReasonMessage,
        ReviewerProfileResponse reviewer,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public static CuratorApplicationResponse from(CuratorApplication application, ReviewerProfileResponse reviewer) {
        return new CuratorApplicationResponse(
                application.getId(),
                ApplicantProfileResponse.from(application.getApplicant()),
//                application.getApplicant().getProfileImageUrl(),
                application.getProgramName(),
                application.getTagline(),
                application.getCuration(),
                application.getStatus(),
                application.getRejectReason(),
                application.getRejectReason() != null ? application.getRejectReason().getMessage() : null,
                reviewer,
                application.getReviewedAt(),
                application.getCreatedAt()
        );
    }
}