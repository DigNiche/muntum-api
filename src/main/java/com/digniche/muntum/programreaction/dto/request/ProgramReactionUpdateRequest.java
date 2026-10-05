package com.digniche.muntum.programreaction.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 프로그램 반응 및 코멘트 변경 요청
 */
public record ProgramReactionUpdateRequest(

        @NotNull(message = "프로그램 반응 상태는 필수입니다.")
        ReactionState reactionState,

        @Size(max = 500, message = "코멘트는 500자 이하여야 합니다.")
        String comment

) {
}