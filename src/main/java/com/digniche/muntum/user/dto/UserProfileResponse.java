package com.digniche.muntum.user.dto;

import com.digniche.muntum.user.entity.User;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 사용자 조회 공통 응답 DTO
 * - '마이페이지(프로필/계정관리) 단건 조회'와 '관리자 사용자 관리 목록 조회'에서 함께 사용
 */
public record UserProfileResponse(
        UUID userId,
        String email,
        String nickname,
        String role,
        long keywordCount,
        long suggestionCount,
        long scrapCount,
        LocalDate joinedAt
) {
    public static UserProfileResponse of(
            User user,
            long keywordCount,
            long suggestionCount,
            long scrapCount
    ) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                keywordCount,
                suggestionCount,
                scrapCount,
                user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : null
        );
    }
}