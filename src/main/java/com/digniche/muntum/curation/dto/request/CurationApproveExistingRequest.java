package com.digniche.muntum.curation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CurationApproveExistingRequest(

        @NotNull(message = "연결할 프로그램 ID는 필수입니다.")
        UUID programId

) {
}