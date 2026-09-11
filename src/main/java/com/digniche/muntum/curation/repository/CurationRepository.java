package com.digniche.muntum.curation.repository;

import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.entity.Curation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurationRepository
        extends JpaRepository<Curation, UUID> {

    /**
     * 내 큐레이션 상태별 조회
     */
    Page<Curation> findByCuratorIdAndStatus(
            UUID curatorId,
            CurationStatus status,
            Pageable pageable
    );

    /**
     * 내 큐레이션 목록 상태별 조회
     */
    Page<Curation> findByProgram_IdAndPublicationStatus(
            UUID programId,
            CurationPublicationStatus publicationStatus,
            Pageable pageable
    );

    /**
     * 내 큐레이션 단건 조회
     */
    Optional<Curation> findByIdAndCuratorId(
            UUID curationId,
            UUID curatorId
    );

    /**
     * 공개 큐레이션 단건 상세
     *
     * 프로그램 ID와 승인 상태까지 함께 확인
     */
    Optional<Curation> findByIdAndProgram_IdAndPublicationStatus(
            UUID curationId,
            UUID programId,
            CurationPublicationStatus publicationStatus
    );
    /**
     * 한 큐레이터가 같은 프로그램에 작성한 큐레이션이 있는지 확인
     */
    boolean existsByProgram_IdAndCuratorId(
            UUID programId,
            UUID curatorId
    );

    /**
     * 승인·반려 동시 요청 방지용 비관적 락
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Curation c
        WHERE c.id = :curationId
    """)
    Optional<Curation> findByIdForUpdate(
            @Param("curationId") UUID curationId
    );
    /**
     * 상태별 목록 조회 추가
     */
    Page<Curation> findAllByStatus(
            CurationStatus status,
            Pageable pageable
    );
    /**
     * 내 큐레이션 전체 조회
     */
    Page<Curation> findAllByCuratorId(
            UUID curatorId,
            Pageable pageable
    );

    /**
     * 전체목록
     */
    List<Curation> findAllByProgram_IdAndPublicationStatusOrderByReviewedAtDesc(
            UUID programId,
            CurationPublicationStatus publicationStatus
    );

    long countByCuratorIdAndStatus(
            UUID curatorId,
            CurationStatus status
    );
}