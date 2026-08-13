package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.UserRole;

import java.util.UUID;

public record CuratorInfoResponse(
    UUID id,
    UserRole role,
    // 프로필 이미지
    String nickname
) {
    public static CuratorInfoResponse from(UUID id, UserRole role, String nickname) {
        return new CuratorInfoResponse(
                id,
                role,
                nickname
        );
    }
}