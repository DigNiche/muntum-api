package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import com.digniche.muntum.program.entity.ProgramType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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
    // 수정 시 동시성 제어용: 동일 프로그램에 대한 동시 PUT 요청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Program p WHERE p.id = :id AND p.deletedAt IS NULL AND p.status IN :statuses")
    Optional<Program> findActiveProgramForUpdate(
            @Param("id") UUID id, @Param("statuses") Collection<ProgramStatus> statuses);

    // 단건
    Optional<Program> findByIdAndDeletedAtIsNullAndStatus(UUID id, ProgramStatus status);

    // 목록
    Page<Program> findByDeletedAtIsNullAndStatus(ProgramStatus status, Pageable pageable);

    // 프로그램 status 조회
    Optional<Program> findByIdAndDeletedAtIsNull(UUID id);

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
    WHERE p.status IN :statuses
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
        WHERE p.status IN :statuses
        AND p.deletedAt IS NULL
        AND pk.keyword.id IN :keywordIds
        GROUP BY p
        ORDER BY COUNT(pk) DESC
    """,
            countQuery = """
        SELECT COUNT(DISTINCT p) FROM Program p
        JOIN ProgramKeyword pk ON pk.program = p
        WHERE p.status IN :statuses
        AND p.deletedAt IS NULL
        AND pk.keyword.id IN :keywordIds
    """
    )
    Page<Program> findProgramsByKeywordIds(
            @Param("statuses") Collection<ProgramStatus> statuses,
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
    WHERE p.status IN :statuses
    AND p.deletedAt IS NULL
    AND pk.keyword.id IN :keywordIds
    AND (:freeOnly IS NULL OR p.free = true)
    AND (:noReservationOnly IS NULL OR p.reserved = false)
    AND (:programType IS NULL OR p.programType = :programType)
    AND (
        :weekStart IS NULL
        OR (
            (p.startDate IS NULL OR p.startDate <= :weekEnd)
            AND (p.endDate IS NULL OR p.endDate >= :weekStart)
        )
    )
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
    WHERE p.status IN :statuses
    AND p.deletedAt IS NULL
    AND pk.keyword.id IN :keywordIds
    AND (:freeOnly IS NULL OR p.free = true)
    AND (:noReservationOnly IS NULL OR p.reserved = false)
    AND (:programType IS NULL OR p.programType = :programType)
    AND (
        :weekStart IS NULL
        OR (
            (p.startDate IS NULL OR p.startDate <= :weekEnd)
            AND (p.endDate IS NULL OR p.endDate >= :weekStart)
        )
    )
"""
    )
    Page<Program> searchProgramsByKeywordIds(
            @Param("statuses") Collection<ProgramStatus> statuses,
            @Param("keywordIds") List<UUID> keywordIds,
            @Param("today") LocalDate today,
            @Param("freeOnly") Boolean freeOnly,
            @Param("noReservationOnly") Boolean noReservationOnly,
            @Param("programType") ProgramType programType,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            Pageable pageable
    );

    // 텍스트 검색: title/tagline/curation LIKE 매칭
    // 정렬: 필드 우선순위(title→tagline→curation) → (안 끝난 것 먼저, 마감임박 → 끝난 것 최근순 → null 맨 뒤)
    @Query(
            value = """
    SELECT p
    FROM Program p
    WHERE p.status IN :statuses
    AND p.deletedAt IS NULL
    AND (
        p.title LIKE :keyword ESCAPE '\\'
        OR p.tagline LIKE :keyword ESCAPE '\\'
        OR p.curation LIKE :keyword ESCAPE '\\'
    )
    AND (:freeOnly IS NULL OR p.free = true)
    AND (:noReservationOnly IS NULL OR p.reserved = false)
    AND (:programType IS NULL OR p.programType = :programType)
    AND (
        :weekStart IS NULL
        OR (
            (p.startDate IS NULL OR p.startDate <= :weekEnd)
            AND (p.endDate IS NULL OR p.endDate >= :weekStart)
        )
    )
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
""",
            countQuery = """
    SELECT COUNT(p)
    FROM Program p
    WHERE p.status IN :statuses
    AND p.deletedAt IS NULL
    AND (
        p.title LIKE :keyword ESCAPE '\\'
        OR p.tagline LIKE :keyword ESCAPE '\\'
        OR p.curation LIKE :keyword ESCAPE '\\'
    )
    AND (:freeOnly IS NULL OR p.free = true)
    AND (:noReservationOnly IS NULL OR p.reserved = false)
    AND (:programType IS NULL OR p.programType = :programType)
    AND (
        :weekStart IS NULL
        OR (
            (p.startDate IS NULL OR p.startDate <= :weekEnd)
            AND (p.endDate IS NULL OR p.endDate >= :weekStart)
        )
    )
"""
    )
    Page<Program> searchProgramsByText(
            @Param("statuses") Collection<ProgramStatus> statuses,
            @Param("keyword") String keyword,
            @Param("today") LocalDate today,
            @Param("freeOnly") Boolean freeOnly,
            @Param("noReservationOnly") Boolean noReservationOnly,
            @Param("programType") ProgramType programType,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            Pageable pageable
    );

    // 입력 좌표로부터 반경 radiusMeters 내의 프로그램 목록 조회
    @Query(value = """
    SELECT * FROM programs
    WHERE status = 'ACTIVE' AND deleted_at IS NULL
    AND latitude IS NOT NULL AND longitude IS NOT NULL
    AND ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) <= :radiusMeters
    ORDER BY ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) ASC
    """,
            countQuery = """
    SELECT COUNT(*) FROM programs
    WHERE status = 'ACTIVE' AND deleted_at IS NULL
    AND latitude IS NOT NULL AND longitude IS NOT NULL
    AND ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) <= :radiusMeters
    """, nativeQuery = true)
    Page<Program> findNearbyPrograms(
            @Param("lat") double lat, @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            Pageable pageable
    );

    // 바운딩 박스 내 프로그램 조회 (지도탭) — 필터칩 적용, 상한 있는 List 반환
    @Query("""
    SELECT p FROM Program p
    WHERE p.status IN :statuses AND p.deletedAt IS NULL
    AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL
    AND p.latitude BETWEEN :swLat AND :neLat
    AND p.longitude BETWEEN :swLng AND :neLng
    AND (:freeOnly IS NULL OR p.free = true)
    AND (:noReservationOnly IS NULL OR p.reserved = false)
    AND (:programType IS NULL OR p.programType = :programType)
    AND (
        :weekStart IS NULL
        OR (
            (p.startDate IS NULL OR p.startDate <= :weekEnd)
            AND (p.endDate IS NULL OR p.endDate >= :weekStart)
        )
    )
    ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Program> findProgramsInBounds(
            @Param("statuses") Collection<ProgramStatus> statuses,
            @Param("swLat") double swLat, @Param("swLng") double swLng,
            @Param("neLat") double neLat, @Param("neLng") double neLng,
            @Param("freeOnly") Boolean freeOnly,
            @Param("noReservationOnly") Boolean noReservationOnly,
            @Param("programType") ProgramType programType,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            Limit limit
    );

    //ACTIVE 상태이고, Deleted null이고, endDate가 오늘보다 이전인 프로그램 조회
    List<Program> findByStatusAndDeletedAtIsNullAndEndDateBefore(
            ProgramStatus status,
            LocalDate date
    );

    // 일반 목록 필터용 메서드 추가
    @Query(
            value = """
    SELECT p
    FROM Program p
    WHERE p.status IN :statuses
    AND p.deletedAt IS NULL
    AND (:freeOnly IS NULL OR p.free = true) 
    AND (:noReservationOnly IS NULL OR p.reserved = false)
    AND (:programType IS NULL OR p.programType = :programType)
    AND (:weekStart IS NULL OR (
                                (p.startDate IS NULL OR p.startDate <= :weekEnd)
                                AND (p.endDate IS NULL OR p.endDate >= :weekStart)
                            )
    )
""",
            countQuery = """
    SELECT COUNT(p)
    FROM Program p
    WHERE p.status IN :statuses
    AND p.deletedAt IS NULL
    AND (:freeOnly IS NULL OR p.free = true)
    AND (:noReservationOnly IS NULL OR p.reserved = false)
    AND (:programType IS NULL OR p.programType = :programType)
    AND (
        :weekStart IS NULL
        OR (
            (p.startDate IS NULL OR p.startDate <= :weekEnd)
            AND (p.endDate IS NULL OR p.endDate >= :weekStart)
        )
    )
"""
    )
    Page<Program> findProgramsWithFilter(
            @Param("statuses") Collection<ProgramStatus> statuses,
            @Param("freeOnly") Boolean freeOnly,
            @Param("noReservationOnly") Boolean noReservationOnly,
            @Param("programType") ProgramType programType,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            Pageable pageable
    );

    // 바운딩 박스 내 프로그램 — 스크랩 수(지금핫한) 정렬. 단일선택이라 HOT일 땐 다른 필터 없음
    @Query("""
    SELECT p FROM Program p
    LEFT JOIN Scrap s ON s.program = p AND s.deletedAt IS NULL
    WHERE p.status IN :statuses AND p.deletedAt IS NULL
    AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL
    AND p.latitude BETWEEN :swLat AND :neLat
    AND p.longitude BETWEEN :swLng AND :neLng
    GROUP BY p
    ORDER BY COUNT(s) DESC, p.createdAt DESC, p.id DESC
    """)
    List<Program> findHotProgramsInBounds(
            @Param("statuses") Collection<ProgramStatus> statuses,
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng,
            Limit limit
    );

    /**
     * 사용자 삭제 시
     * - 생성자, 수정자, 삭제자 System UUID로 채우기
     */

    @Modifying
    @Query("UPDATE Program p SET p.createdBy = :systemUuid WHERE p.createdBy = :userId")
    void replaceCreatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Program p SET p.updatedBy = NULL WHERE p.updatedBy = :userId")
    void replaceUpdatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Program p SET p.deletedBy = :systemUuid WHERE p.deletedBy = :userId")
    void replaceDeletedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

}