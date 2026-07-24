package com.digniche.muntum.scrap.repository;

import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.scrap.entity.Scrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 스크랩 데이터 접근 계층
 */
public interface ScrapRepository extends JpaRepository<Scrap, UUID> {

    Optional<Scrap> findByUserIdAndProgramId(UUID userId, UUID programId);

    /**
     * 내 스크랩 목록 조회
     * Program을 fetch join하여 N+1 문제를 방지한다.
     * 삭제된 프로그램(p.deletedAt)은 제외한다.
     * countQuery도 동일 조건으로 맞춰 content 개수와 totalElements가 어긋나지 않게 한다.
     */
    @Query(
            value = "SELECT s FROM Scrap s JOIN FETCH s.program p " +
                    "WHERE s.user.id = :userId " +
                    "AND p.deletedAt IS NULL " +
                    "AND p.status IN :statuses",
            countQuery = "SELECT COUNT(s) FROM Scrap s JOIN s.program p " +
                    "WHERE s.user.id = :userId " +
                    "AND p.deletedAt IS NULL " +
                    "AND p.status IN :statuses"
    )
    Page<Scrap> findMyScrapsWithProgram(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<ProgramStatus> statuses,
            Pageable pageable
    );

    /**
     * 사용자의 스크랩 모두 삭제
     */
    @Modifying
    @Query("DELETE FROM Scrap s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    /**
     * 사용자별 스크랩 개수 집계 (프로필/사용자 관리 조회용)
     */
    @Query("""
    SELECT s.user.id, COUNT(s)
    FROM Scrap s
    WHERE s.user.id IN :userIds
    GROUP BY s.user.id
""")
    List<Object[]> countByUserIds(@Param("userIds") Collection<UUID> userIds);

}
