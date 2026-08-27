package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CuratorApplicationResponse(
        UUID id,
        UUID applicantId,
        String applicantNickname,
        String applicantEmail,
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
                application.getApplicant().getId(),
                application.getApplicant().getNickname(),
                application.getApplicant().getEmail(),
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