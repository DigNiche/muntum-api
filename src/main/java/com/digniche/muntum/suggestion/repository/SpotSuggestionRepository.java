package com.digniche.muntum.suggestion.repository;

import com.digniche.muntum.suggestion.entity.SpotSuggestion;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<SpotSuggestion> findByInformer(UUID informer, Pageable pageable);

    // 관리자용 전체 목록 조회 - 상태 필터
    Page<SpotSuggestion> findByStatusAndDeletedAtIsNull(SuggestionStatus status, Pageable pageable);

    // 관리자용 전체 목록 조회 - 상태 필터 없음
    Page<SpotSuggestion> findAllByDeletedAtIsNull(Pageable pageable);

    // 관리자용 삭제된 목록 조회
    Page<SpotSuggestion> findByDeletedAtIsNotNull(Pageable pageable);

    /**
     * 사용자 삭제 시
     * - 생성자 / 수정자 / 제보자 / 검토자 / 삭제자 Withdrawn UUID로 채우기
     */

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.createdBy = :withdrawnUuid WHERE s.createdBy = :userId")
    void replaceCreatedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.updatedBy = :withdrawnUuid WHERE s.updatedBy = :userId")
    void replaceUpdatedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.informer = :withdrawnUuid WHERE s.informer = :userId")
    void replaceInformerWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.reviewedBy = :withdrawnUuid WHERE s.reviewedBy = :userId")
    void replaceReviewedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.deletedBy = :withdrawnUuid WHERE s.deletedBy = :userId")
    void replaceDeletedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);


    // 사용자별 제보 개수 집계 (프로필/사용자 관리 조회용)
    @Query("""
    SELECT s.informer, COUNT(s)
    FROM SpotSuggestion s
    WHERE s.informer IN :userIds
    GROUP BY s.informer
""")
    List<Object[]> countByInformerIds(@Param("userIds") Collection<UUID> userIds);
}