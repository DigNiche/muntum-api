package com.digniche.muntum.curationlike.repository;

import com.digniche.muntum.curationlike.entity.CurationLike;
import com.digniche.muntum.curationlike.entity.CurationLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * CurationLike 레포지토리
 * - Lock 순서: 큐레이션 행 -> 좋아요 행
 */
public interface CurationLikeRepository extends JpaRepository<CurationLike, CurationLikeId> {

    /**
     * 좋아요가 없을 때만 추가
     * - 실제로 추가했으면 1, 이미 있으면 0 반환
     * - JPQL은 INSERT ... SELECT ... WHERE NOT EXISTS를 지원하지 않아 네이티브 쿼리 사용
     */
    @Modifying
    @Query(
            value = """
            INSERT INTO curation_likes (user_id, curation_id, created_at)
            SELECT :userId, :curationId, :createdAt
            FROM DUAL
            WHERE NOT EXISTS (
                SELECT 1
                FROM curation_likes l
                WHERE l.user_id = :userId
                  AND l.curation_id = :curationId
            )
            """,
            nativeQuery = true
    )
    int insertIfAbsent(@Param("userId") UUID userId, @Param("curationId") UUID curationId, @Param("createdAt") LocalDateTime createdAt);


    /**
     * 좋아요 삭제
     * - 실제로 지웠으면 1, 없었으면 0 반환
     */
    @Modifying
    @Query("""
        DELETE FROM CurationLike l
        WHERE l.id.userId = :userId
          AND l.id.curationId = :curationId
    """)
    int deleteByUserIdAndCurationId(@Param("userId") UUID userId, @Param("curationId") UUID curationId);


    /**
     * 주어진 큐레이션 중 사용자가 좋아요를 누른 큐레이션 id
     * - 목록의 큐레이션마다 따로 조회하지 않도록(N+1 방지) IN 조회 1번
     */
    @Query("""
        SELECT l.id.curationId
        FROM CurationLike l
        WHERE l.id.userId = :userId
          AND l.id.curationId IN :curationIds
    """)
    List<UUID> findLikedCurationIds(@Param("userId") UUID userId, @Param("curationIds") Collection<UUID> curationIds);
}
