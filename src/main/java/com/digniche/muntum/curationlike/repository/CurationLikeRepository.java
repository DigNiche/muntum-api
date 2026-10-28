package com.digniche.muntum.curationlike.repository;

import com.digniche.muntum.curationlike.entity.CurationLike;
import com.digniche.muntum.curationlike.entity.CurationLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CurationLike 레포지토리
 * - Lock 순서: 큐레이션 행 -> 좋아요 행
 */
public interface CurationLikeRepository extends JpaRepository<CurationLike, CurationLikeId> {

    /**
     * 좋아요가 없을 때만 추가
     * - 실제로 추가했으면 1, 이미 있으면 0 반환
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
}
