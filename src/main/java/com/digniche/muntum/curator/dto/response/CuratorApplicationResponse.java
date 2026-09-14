package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.user.dto.response.UserProfileResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 큐레이터 지원서 상세 조회 응답 DTO
 */
public record CuratorApplicationResponse(
        UUID id,
        UserProfileResponse applicant,
        ApplicationContentResponse portfolio,
        ApplicantStatusInfoResponse statusInfo,
        ReviewerProfileResponse reviewer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CuratorApplicationResponse from(CuratorApplication application, ReviewerProfileResponse reviewer) {
        return new CuratorApplicationResponse(
                application.getId(),
                UserProfileResponse.from(application.getApplicant()),
                ApplicationContentResponse.from(application),
                ApplicantStatusInfoResponse.from(application),
                reviewer,
                application.getCreatedAt(),
                application.getUpdatedAt()

        );
    }
}