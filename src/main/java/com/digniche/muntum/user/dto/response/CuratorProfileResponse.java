package com.digniche.muntum.user.dto.response;

import java.util.UUID;

public record CuratorProfileResponse(
        UUID curatorId,
        String role,
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