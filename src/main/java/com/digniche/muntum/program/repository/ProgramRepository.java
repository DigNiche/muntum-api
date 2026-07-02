package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

import java.util.Optional;
import java.util.UUID;

/**
 * 프로그램 데이터 접근 계층
 */
public interface ProgramRepository extends JpaRepository<Program, UUID> {

    // 단건
    Optional<Program> findByIdAndDeletedAtIsNullAndStatus(UUID id, ProgramStatus status);

    // 목록
    Page<Program> findByDeletedAtIsNullAndStatus(ProgramStatus status, Pageable pageable);

    Optional<Program> findByIdAndDeletedAtIsNullAndStatusIn(
            UUID id, Collection<ProgramStatus> statuses);

    @Query("SELECT p FROM Program p " +
            "WHERE p.deletedAt IS NULL " +
            "AND p.status IN :statuses " +
            "ORDER BY CASE WHEN p.endDate IS NOT NULL AND p.endDate < :today THEN 1 ELSE 0 END ASC")
    Page<Program> findProgramsEndedLast(
            @Param("statuses") Collection<ProgramStatus> statuses,
            @Param("today") LocalDate today,
            Pageable pageable);
    // 목록 조회 - 삭제되지 않은 프로그램만, 페이징 적용
    Page<Program> findByDeletedAtIsNull(Pageable pageable);
    Page<Program> findByStatusAndDeletedAtIsNull(ProgramStatus status, Pageable pageable);

    // 마감일이 이번달인 목록 중 마감일이 오늘 날짜와 가까운 순으로 정렬
    @Query("""
    SELECT p FROM Program p
    WHERE p.status = :status
    AND p.deletedAt IS NULL
    AND p.endDate IS NOT NULL
    AND p.endDate >= :today
    AND p.endDate <= :monthEnd
    ORDER BY ABS(DATEDIFF(p.endDate, :today)) ASC
""")
    Page<Program> findByStatusOrderByClosestEndDate(@Param("status") ProgramStatus status, @Param("today") LocalDate today, @Param("monthEnd") LocalDate monthEnd, Pageable pageable);


}