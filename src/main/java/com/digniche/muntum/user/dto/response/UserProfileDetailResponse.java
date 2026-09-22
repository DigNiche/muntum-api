package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.User;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 사용자 상세 정보 조회 응답 DTO
 * (이전 및 삭제 예정 : UserActivityResponse, UserProfileImageResponse, UserProfileResponse로 전환)
 */
public record UserProfileDetailResponse(
        UUID userId,
        String email,
        String nickname,
        String role,
        long keywordCount,
        long suggestionCount,
        long scrapCount,
        String profileImageUrl,
        LocalDate joinedAt
) {
    public static UserProfileDetailResponse from(
            User user,
            long keywordCount,
            long suggestionCount,
            long scrapCount
    ) {
        return new UserProfileDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                keywordCount,
                suggestionCount,
                scrapCount,
                user.getProfileImageUrl(),
                user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null
        );
    }
}