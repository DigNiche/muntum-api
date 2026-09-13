package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 기본 정보 조회 응답 DTO
 */
public record UserProfileResponse(
        UUID userId,
        String email,
        String nickname,
        String profileImageUrl,
        String role,
        String status,
        LocalDate joinedAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null
        );
    }
}