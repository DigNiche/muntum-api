package com.digniche.muntum.program.dto.request;

import com.digniche.muntum.program.entity.ProgramStatus;
import jakarta.validation.constraints.NotNull;

public record ProgramStatusUpdateRequest(
        @NotNull(message = "프로그램 상태는 필수입니다.")
        ProgramStatus status
) {
}