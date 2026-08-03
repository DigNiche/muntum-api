package com.digniche.muntum.programreaction.repository;

import com.digniche.muntum.programreaction.entity.ProgramReaction;
import com.digniche.muntum.programreaction.entity.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.digniche.muntum.program.entity.ProgramStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 프로그램 좋아요·싫어요 데이터 접근 계층
 */
public interface ProgramReactionRepository
        extends JpaRepository<ProgramReaction, UUID> {

    /**
     * 특정 사용자가 특정 프로그램에 남긴 현재 반응 조회
     */
    Optional<ProgramReaction> findByUserIdAndProgramId(
            UUID userId,
            UUID programId
    );

    /**
     * 특정 프로그램에 대한 사용자의 반응 삭제
     *
     * NONE 요청 처리용
     * 삭제할 반응이 없으면 0 반환
     */
    @Modifying
    @Query("""
        DELETE FROM ProgramReaction r
        WHERE r.user.id = :userId
        AND r.program.id = :programId
    """)
    int deleteByUserIdAndProgramId(
            @Param("userId") UUID userId,
            @Param("programId") UUID programId
    );

    /**
     * 내 좋아요 또는 싫어요 프로그램 목록 조회
     *
     * Program을 fetch join하여 반응마다 프로그램 조회 쿼리가
     * 추가로 실행되는 N+1 문제를 방지
     * 삭제된 프로그램 제외
     * 정렬과 페이징은 Pageable에서 적용
     */
    @Query(
            value = """
            SELECT r
            FROM ProgramReaction r
            JOIN FETCH r.program p
            WHERE r.user.id = :userId
            AND r.reactionType = :reactionType
            AND p.deletedAt IS NULL
            AND p.status IN :statuses
        """,
            countQuery = """
            SELECT COUNT(r)
            FROM ProgramReaction r
            JOIN r.program p
            WHERE r.user.id = :userId
            AND r.reactionType = :reactionType
            AND p.deletedAt IS NULL
            AND p.status IN :statuses
        """
    )
    Page<ProgramReaction> findMyReactionsWithProgram(
            @Param("userId") UUID userId,
            @Param("reactionType") ReactionType reactionType,
            @Param("statuses") Collection<ProgramStatus> statuses,
            Pageable pageable
    );

    /**
     * 특정 프로그램의 좋아요·싫어요 개수를 한 번에 집계
     */
    @Query("""
        SELECT
            r.reactionType AS reactionType,
            COUNT(r) AS reactionCount
        FROM ProgramReaction r
        WHERE r.program.id = :programId
        GROUP BY r.reactionType
    """)
    List<ProgramReactionCountProjection>
    countByProgramIdGroupByReactionType(
            @Param("programId") UUID programId
    );

    /**
     * 회원 탈퇴 시 사용자의 모든 프로그램 반응 삭제
     */
    @Modifying
    @Query("""
        DELETE FROM ProgramReaction r
        WHERE r.user.id = :userId
    """)
    int deleteAllByUserId(
            @Param("userId") UUID userId
    );
}