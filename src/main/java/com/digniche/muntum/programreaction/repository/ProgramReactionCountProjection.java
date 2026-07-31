package com.digniche.muntum.programreaction.repository;

import com.digniche.muntum.programreaction.entity.ReactionType;

/**
 * 프로그램별 좋아요·싫어요 개수 집계 결과
 */
public interface ProgramReactionCountProjection {

    ReactionType getReactionType();

    long getReactionCount();
}
