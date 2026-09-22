package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.user.dto.response.UserProfileResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 큐레이터 지원서 목록 카드 조회 응답 DTO
 */
public record CuratorApplicationCardResponse(
        UUID id,
        UserProfileResponse applicant,
        ApplicantStatusInfoResponse statusInfo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CuratorApplicationCardResponse from(CuratorApplication application) {
        return new CuratorApplicationCardResponse(
                application.getId(),
                UserProfileResponse.from(application.getApplicant()),
                ApplicantStatusInfoResponse.from(application),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}