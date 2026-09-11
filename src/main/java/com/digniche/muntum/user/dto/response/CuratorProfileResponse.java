package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.UserRole;

import java.util.UUID;

public record CuratorProfileResponse(
        UUID curatorId,
        String role,
        String nickname,
        String profileImageUrl
) {
    /**
     * 기존 코드 호환용
     */
    public static CuratorProfileResponse from(UUID curatorId, String role, String nickname) {
        return new CuratorProfileResponse(
                curatorId,
                role,
                nickname,
                null
        );
    }
    /**
     * 프로필 이미지 + 큐레이션 수 포함
     */
    public static CuratorProfileResponse from(
            UUID curatorId,
            String role,
            String nickname,
            String profileImageUrl
    ) {
        return new CuratorProfileResponse(
                curatorId,
                role,
                nickname,
                profileImageUrl
        );
    }

}