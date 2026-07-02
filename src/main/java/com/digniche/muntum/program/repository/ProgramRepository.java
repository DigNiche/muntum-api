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

import java.util.List;
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

    // 인기 키워드를 많이 가진 프로그램 순으로 정렬
    @Query(
            value = """
        SELECT p FROM Program p
        JOIN ProgramKeyword pk ON pk.program = p
        WHERE p.status = :status
        AND p.deletedAt IS NULL
        AND pk.keyword.id IN :keywordIds
        GROUP BY p
        ORDER BY COUNT(pk) DESC
    """,
            countQuery = """
        SELECT COUNT(DISTINCT p) FROM Program p
        JOIN ProgramKeyword pk ON pk.program = p
        WHERE p.status = :status
        AND p.deletedAt IS NULL
        AND pk.keyword.id IN :keywordIds
    """
    )
    Page<Program> findProgramsByKeywordIds(
            @Param("status") ProgramStatus status,
            @Param("keywordIds") List<UUID> keywordIds,
            Pageable pageable
    );

    // 키워드 검색: 사용자가 직접 선택한 keywordIds와 많이 매칭된 프로그램 순
    // 정렬: 매칭 수 DESC → 안 끝난 것 먼저(마감임박순) → 끝난 것(최근 종료순) → 미입력 맨 뒤
    @Query(
            value = """
        SELECT p
        FROM Program p
        JOIN ProgramKeyword pk ON pk.program = p
        WHERE p.status = :status
        AND p.deletedAt IS NULL
        AND pk.keyword.id IN :keywordIds
        GROUP BY p
        ORDER BY COUNT(pk) DESC,
                 CASE
                     WHEN p.endDate IS NULL THEN 2
                     WHEN p.endDate < :today THEN 1
                     ELSE 0
                 END ASC,
                 CASE
                     WHEN p.endDate >= :today THEN p.endDate
                 END ASC,
                 p.endDate DESC
    """,
            countQuery = """
        SELECT COUNT(DISTINCT p)
        FROM Program p
        JOIN ProgramKeyword pk ON pk.program = p
        WHERE p.status = :status
        AND p.deletedAt IS NULL
        AND pk.keyword.id IN :keywordIds
    """
    )
    Page<Program> searchProgramsByKeywordIds(
            @Param("status") ProgramStatus status,
            @Param("keywordIds") List<UUID> keywordIds,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    // 텍스트 검색: title/tagline/curation LIKE 매칭
    // 정렬: 필드 우선순위(title→tagline→curation) → (안 끝난 것 먼저, 마감임박 → 끝난 것 최근순 → null 맨 뒤)
    @Query("""
        SELECT p FROM Program p
        WHERE p.status = :status
        AND p.deletedAt IS NULL
        AND (p.title LIKE :keyword ESCAPE '\\'
             OR p.tagline LIKE :keyword ESCAPE '\\'
             OR p.curation LIKE :keyword ESCAPE '\\')
        ORDER BY
            CASE WHEN p.title LIKE :keyword ESCAPE '\\' THEN 0
                 WHEN p.tagline LIKE :keyword ESCAPE '\\' THEN 1
                 WHEN p.curation LIKE :keyword ESCAPE '\\' THEN 2
                 ELSE 3 END ASC,
            CASE WHEN p.endDate IS NULL THEN 2
                 WHEN p.endDate < :today THEN 1
                 ELSE 0 END ASC,
            CASE WHEN p.endDate >= :today THEN p.endDate END ASC,
            p.endDate DESC
    """)
    Page<Program> searchProgramsByText(
            @Param("status") ProgramStatus status,
            @Param("keyword") String keyword,
            @Param("today") LocalDate today,
            Pageable pageable
    );
}