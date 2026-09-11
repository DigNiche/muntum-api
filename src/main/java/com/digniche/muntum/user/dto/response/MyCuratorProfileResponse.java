package com.digniche.muntum.user.dto.response;

import java.util.UUID;

public record MyCuratorProfileResponse(
        UUID curatorId,
        String role,
        String nickname,
        String profileImageUrl,

        long approvedCount,
        long pendingCount,
        long changesRequestedCount
) {
}