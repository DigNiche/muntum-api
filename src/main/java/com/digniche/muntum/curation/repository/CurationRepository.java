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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurationRepository
        extends JpaRepository<Curation, UUID> {

    /**
     * 내 큐레이션 목록 조회
     */
    Page<Curation> findByCuratorId(
            UUID curatorId,
            Pageable pageable
    );

    /**
     * 내 큐레이션 목록 상태별 조회
     */
    Page<Curation> findByCuratorIdAndStatus(
            UUID curatorId,
            CurationStatus status,
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
     * 관리자 심사 목록 상태별 조회
     */
    Page<Curation> findByStatus(
            CurationStatus status,
            Pageable pageable
    );

    /**
     * 프로그램별 큐레이션 목록
     *
     * 공개 API에서는 반드시 status=APPROVED 전달
     */
    Page<Curation> findByProgram_IdAndStatus(
            UUID programId,
            CurationStatus status,
            Pageable pageable
    );

    /**
     * 공개 큐레이션 단건 상세
     *
     * 프로그램 ID와 승인 상태까지 함께 확인
     */
    Optional<Curation> findByIdAndProgram_IdAndStatus(
            UUID curationId,
            UUID programId,
            CurationStatus status
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
        FROM ProgramCuration c
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
     * 여러 사용자의 큐레이션 개수 일괄 집계
     *
     * 프로필 및 사용자 관리 조회에서 N+1을 방지
     * 상태 조건이 없으므로 대기·승인·반려를 모두 포함
     */
    @Query("""
        SELECT c.curatorId, COUNT(c)
        FROM Curation c
        WHERE c.curatorId IN :curatorIds
        GROUP BY c.curatorId
    """)
    List<Object[]> countByCuratorIds(
            @Param("curatorIds")
            Collection<UUID> curatorIds
    );

    Page<Curation> findAllByCuratorId(
            UUID curatorId,
            Pageable pageable
    );
}