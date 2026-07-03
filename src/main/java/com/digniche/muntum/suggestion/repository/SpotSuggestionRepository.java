package com.digniche.muntum.suggestion.repository;

import com.digniche.muntum.suggestion.entity.SpotSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpotSuggestionRepository extends JpaRepository<SpotSuggestion, UUID> {

    // 생성자: System UUID로 채우기
    @Modifying
    @Query(value = "UPDATE spot_suggestions SET created_by = :systemUuid WHERE created_by = :userId", nativeQuery = true)
    void replaceCreatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    // 수정자: Null 처리
    @Modifying
    @Query(value = "UPDATE spot_suggestions SET updated_by = NULL WHERE updated_by = :userId", nativeQuery = true)
    void nullifyUpdatedBy(@Param("userId") UUID userId);

    // 제보자: Null 처리
    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.informer = null WHERE s.informer.id = :userId")
    void nullifyInformerByUserId(@Param("userId") UUID userId);

    // 검토자: Null 처리
    @Modifying
    @Query("UPDATE SpotSuggestion s SET s.reviewedBy = null WHERE s.reviewedBy.id = :userId")
    void nullifyReviewedByUserId(@Param("userId") UUID userId);
}