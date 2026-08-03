package com.digniche.muntum.programreaction.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 프로그램 좋아요·싫어요 변경 요청 DTO
 */
public record ProgramReactionUpdateRequest(

        @NotNull(message = "프로그램 반응 상태는 필수입니다.")
        ReactionState reactionState

) {
}