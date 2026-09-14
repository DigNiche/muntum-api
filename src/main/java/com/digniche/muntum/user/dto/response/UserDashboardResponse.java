package com.digniche.muntum.user.dto.response;

/**
 * 사용자 프로필 정보와 활동 정보를 모두 포함한 응답 DTO
 */
public record UserDashboardResponse (
    UserProfileResponse user,
    UserActivityResponse activities
){
    public static UserDashboardResponse from(UserProfileResponse user, UserActivityResponse activities) {
        return new UserDashboardResponse(user, activities);
    }
}
