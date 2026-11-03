package com.digniche.muntum.curation.repository;

import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.entity.Curation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.program.entity.ProgramStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 큐레이션 레포지토리
 */
public interface CurationRepository extends JpaRepository<Curation, UUID> {

    /**
     * 내 큐레이션 상태별 조회
     */
    Page<Curation> findByCuratorIdAndStatusAndDeletedAtIsNull(UUID curatorId, CurationStatus status, Pageable pageable);

    /**
     * 큐레이션 목록 상태별 조회
     */
    Page<Curation> findByProgram_IdAndPublicationStatusAndDeletedAtIsNull(UUID programId, CurationPublicationStatus publicationStatus, Pageable pageable);

    /**
     * 내 큐레이션 단건 조회
     */
    Optional<Curation> findByIdAndCuratorIdAndDeletedAtIsNull(UUID curationId, UUID curatorId);

    /**
     * 공개 큐레이션 단건 상세
     * 프로그램 ID와 승인 상태까지 함께 확인
     */
    Optional<Curation> findByIdAndProgram_IdAndPublicationStatusAndDeletedAtIsNull(UUID curationId, UUID programId, CurationPublicationStatus publicationStatus);

    /**
     * 한 큐레이터가 같은 프로그램에 작성한 큐레이션이 있는지 확인
     */
    boolean existsByProgram_IdAndCuratorIdAndIdNotAndDeletedAtIsNull(UUID programId, UUID curatorId, UUID curationId);

    /**
     * 승인·반려 동시 요청 방지용 비관적 락
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Curation c
        WHERE c.id = :curationId
          AND c.deletedAt IS NULL
    """)
    Optional<Curation> findByIdForUpdate(@Param("curationId") UUID curationId);

    /**
     * 상태별 목록 조회 추가
     */
    Page<Curation> findAllByStatusAndDeletedAtIsNull(CurationStatus status, Pageable pageable);

    /**
     * 내 큐레이션 전체 조회
     */
    Page<Curation> findAllByCuratorIdAndDeletedAtIsNull(UUID curatorId, Pageable pageable);

    /**
     * 전체목록
     */
    List<Curation> findAllByProgram_IdAndPublicationStatusAndDeletedAtIsNullOrderByReviewedAtDesc(UUID programId, CurationPublicationStatus publicationStatus);

    long countByCuratorIdAndStatusAndDeletedAtIsNull(UUID curatorId, CurationStatus status);

    Optional<Curation> findByIdAndDeletedAtIsNull(UUID curationId);


    /**
     * 좋아요 누를 수 있는 큐레이션 기준
     * - 공개 프로그램 + 게시된 큐레이션
     */
    String CURATION_LIKEABLE_CONDITION = """
          c.id = :curationId
          AND c.publicationStatus = :publicationStatus
          AND c.deletedAt IS NULL
          AND EXISTS (
              SELECT 1
              FROM Program p
              WHERE p.id = c.program.id
                AND p.deletedAt IS NULL
                AND p.status IN :programStatuses
          )
    """;


    /**
     * 큐레이션 단건 잠금 조회 (좋아요 및 좋아요 취소용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Curation c WHERE " + CURATION_LIKEABLE_CONDITION)
    Optional<Curation> findLikeableByIdForUpdate(
            @Param("curationId") UUID curationId,
            @Param("publicationStatus") CurationPublicationStatus publicationStatus,
            @Param("programStatuses") List<ProgramStatus> programStatuses
    );


    /**
     * 큐레이션 좋아요 수 증가 (DB에서 원자적으로)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Curation c
        SET c.likeCount = c.likeCount + 1
        WHERE c.id = :curationId
    """)
    int increaseLikeCount(@Param("curationId") UUID curationId);


    /**
     * 큐레이션 좋아요 수 감소 (DB에서 원자적으로)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Curation c
        SET c.likeCount = c.likeCount - 1
        WHERE c.id = :curationId
    """)
    int decreaseLikeCount(@Param("curationId") UUID curationId);


    /**
     * 큐레이션과 프로그램을 상태 조건 없이 함께 조회 (좋아요 불가 이유 분류용)
     */
    @Query("""
    SELECT c
    FROM Curation c
    LEFT JOIN FETCH c.program
    WHERE c.id = :curationId
""")
    Optional<Curation> findWithProgramById(@Param("curationId") UUID curationId);

}