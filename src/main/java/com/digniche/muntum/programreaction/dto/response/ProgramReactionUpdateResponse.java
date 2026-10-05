package com.digniche.muntum.programreaction.dto.response;

import com.digniche.muntum.programreaction.entity.ReactionType;

/**
 * 프로그램 반응 변경 결과 DTO
 */
public record ProgramReactionUpdateResponse(
        ReactionType myReaction,
        String myComment
) {
    public static ProgramReactionUpdateResponse from(
            ReactionType myReaction,
            String myComment
    ) {
        return new ProgramReactionUpdateResponse(myReaction, myComment);
    }
}