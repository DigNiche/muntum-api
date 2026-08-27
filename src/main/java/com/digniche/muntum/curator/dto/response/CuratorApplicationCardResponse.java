package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 큐레이터 지원서 목록 카드 조회 응답 DTO
 */
public record CuratorApplicationCardResponse(
        UUID id,
        String programName,
        String tagline,
        String applicantNickname,
//        String applicantProfileImageUrl,
        CuratorApplicationStatus status,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public static CuratorApplicationCardResponse from(CuratorApplication application) {
        return new CuratorApplicationCardResponse(
                application.getId(),
                application.getProgramName(),
                application.getTagline(),
                application.getApplicant().getNickname(),
//                application.getApplicant().getProfileImageUrl(),
                application.getStatus(),
                application.getReviewedAt(),
                application.getCreatedAt()
        );
    }
}