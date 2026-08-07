package com.digniche.muntum.programreaction.dto.response;

import com.digniche.muntum.programreaction.entity.ReactionType;

/**
 * 프로그램 상세 화면의 반응 정보
 */
public record ProgramReactionSummaryResponse(
        ReactionType myReaction,
        long likeCount,
        long dislikeCount
) {
}