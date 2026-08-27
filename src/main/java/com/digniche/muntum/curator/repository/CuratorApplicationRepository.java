package com.digniche.muntum.curator.repository;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CuratorApplicationRepository extends JpaRepository<CuratorApplication, UUID> {

    // 사용자의 지원 내역 중 가장 최근 1건 조회 (최신순 정렬)
    Optional<CuratorApplication> findFirstByApplicant_IdOrderByCreatedAtDesc(UUID applicantId);

    // 사용자의 지원 내역 전체 조회 (최신순 정렬)
    Page<CuratorApplication> findByApplicant_IdOrderByCreatedAtDesc(UUID applicantId, Pageable pageable);

    // 지원 상태 필터링하여 조회
    Page<CuratorApplication> findByStatus(CuratorApplicationStatus status, Pageable pageable);

    // 특정 지원자가 지원한 내역 중 {status}인 지원서가 존재하는지 여부
    boolean existsByApplicant_IdAndStatus(UUID applicantId, CuratorApplicationStatus status);



    /**
     * 사용자(관리자) 탈퇴 시, Withdrawn UUID로 채우기
     * - 검토자 Withdrawn UUID로 채우기
     */
    @Modifying
    @Query("UPDATE CuratorApplication ca SET ca.reviewedBy = :withdrawnUuid WHERE ca.reviewedBy = :userId")
    void replaceReviewedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    @Modifying
    @Query("UPDATE CuratorApplication ca SET ca.updatedBy = :withdrawnUuid WHERE ca.updatedBy = :userId")
    void replaceUpdatedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    /**
     * 사용자(큐레이터) 탈퇴 시, 지원 내역 모두 삭제
     */
    @Modifying
    @Query("DELETE FROM CuratorApplication ca WHERE ca.applicant.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}