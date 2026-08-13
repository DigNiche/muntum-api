package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.UserRole;

import java.util.UUID;

public record CuratorProfileResponse(
        UUID curatorId,
        String role,
        // TODO: 프로필 이미지
        String nickname
) {
    public static CuratorProfileResponse from(UUID curatorId, String role, String nickname) {
        return new CuratorProfileResponse(
                curatorId,
                role,
                nickname
        );
    }
}