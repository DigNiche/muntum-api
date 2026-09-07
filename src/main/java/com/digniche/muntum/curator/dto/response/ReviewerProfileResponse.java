package com.digniche.muntum.curator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * 검토자 기본 프로필 정보 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
