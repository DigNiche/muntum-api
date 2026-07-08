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
import java.util.Optional;
import java.util.UUID;

/**
 * 스크랩 데이터 접근 계층
 */
public interface ScrapRepository extends JpaRepository<Scrap, UUID> {

    /**
     * 재등록 분기용
     * 삭제 여부와 상관없이 특정 사용자-프로그램 스크랩 row를 찾는다.
     * (restore 대상을 잡아야 하므로 deletedAt 조건을 넣지 않는다.)
     */
    Optional<Scrap> findByUserIdAndProgramId(UUID userId, UUID programId);
    Optional<Scrap> findByUserIdAndProgramIdAndDeletedAtIsNull(UUID userId, UUID programId);
    /**
     * 내 스크랩 목록 조회
     * Program을 fetch join하여 N+1 문제를 방지한다.
     * 삭제된 스크랩(s.deletedAt)과 삭제된 프로그램(p.deletedAt)은 제외한다.
     * countQuery도 동일 조건으로 맞춰 content 개수와 totalElements가 어긋나지 않게 한다.
     */
    @Query(
            value = "SELECT s FROM Scrap s JOIN FETCH s.program p " +
                    "WHERE s.user.id = :userId " +
                    "AND s.deletedAt IS NULL " +
                    "AND p.deletedAt IS NULL " +
                    "AND p.status IN :statuses",
            countQuery = "SELECT COUNT(s) FROM Scrap s JOIN s.program p " +
                    "WHERE s.user.id = :userId " +
                    "AND s.deletedAt IS NULL " +
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


}