package com.digniche.muntum.user.dto.response;

/**
 * 프로필 이미지 응답 DTO
 */
public record UserProfileImageResponse(
        String profileImageUrl
) {
    public static UserProfileImageResponse of(String profileImageUrl) {
        return new UserProfileImageResponse(profileImageUrl);
    }
}