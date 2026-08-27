package com.digniche.muntum.curator.dto.response;

import java.util.UUID;

/**
 * 사용자 기본 프로필 정보 응답 DTO
 */
public record ReviewerProfileResponse(
        UUID userId,
        String nickname,
        String email
) {
    public static ReviewerProfileResponse from(UUID userId, String nickname, String email) {
        return new ReviewerProfileResponse(
                userId,
                nickname,
                email
        );
    }
}
