package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.user.entity.User;

import java.util.UUID;

/**
 * 지원자 기본 프로필 정보 응답 DTO
 */
public record ApplicantProfileResponse(
        UUID userId,
        String nickname,
        String email
//        String profileImageUrl
) {
    public static ApplicantProfileResponse from(User applicant) {
        return new ApplicantProfileResponse(
                applicant.getId(),
                applicant.getNickname(),
                applicant.getEmail()
//                applicant.getProfileImageUrl()
        );
    }
}
