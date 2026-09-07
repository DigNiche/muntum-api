package com.digniche.muntum.curator.dto.request;

import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record CuratorApplicationStatusUpdateRequest(

        @NotNull
        CuratorApplicationStatus status,

        CuratorApplicationRejectReason rejectReason
) {
}