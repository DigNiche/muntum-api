package com.digniche.muntum.suggestion.repository;

import com.digniche.muntum.suggestion.entity.SpotSuggestion;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpotSuggestionRepository extends JpaRepository<SpotSuggestion, UUID> {

    /**
     * 제보 조회
     */

    // 내 제보 목록 조회
    @EntityGraph(attributePaths = {"informer", "reviewedBy"})
    Page<SpotSuggestion> findByInformer_Id(UUID informerId, Pageable pageable);

    // 관리자용 전체 목록 조회 - 상태 필터
    @EntityGraph(attributePaths = {"informer", "reviewedBy"})
    Page<SpotSuggestion> findByStatus(SuggestionStatus status, Pageable pageable);

    // 관리자용 전체 목록 조회 - 상태 필터 없음 (N+1 방지용 fetch join)
    @EntityGraph(attributePaths = {"informer", "reviewedBy"})
    Page<SpotSuggestion> findAllBy(Pageable pageable);

    /**
     * 사용자 삭제 시
     * - 생성자 / 수정자 / 제보자 / 검토자 System UUID로 채우기
     */

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.createdBy = :systemUuid WHERE s.createdBy = :userId")
    void replaceCreatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.updatedBy = NULL WHERE s.updatedBy = :userId")
    void replaceUpdatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.informer = null WHERE s.informer.id = :userId")
    void replaceInformerWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.reviewedBy = null WHERE s.reviewedBy.id = :userId")
    void replaceReviewedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    // 사용자별 제보 개수 집계 (프로필/사용자 관리 조회용)
    @Query("""
    SELECT s.informer.id, COUNT(s)
    FROM SpotSuggestion s
    WHERE s.informer.id IN :userIds
    GROUP BY s.informer.id
""")
    List<Object[]> countByInformerIds(@Param("userIds") Collection<UUID> userIds);
}