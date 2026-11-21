package com.digniche.muntum.curationlike.repository;

import com.digniche.muntum.curationlike.entity.CurationLike;
import com.digniche.muntum.curationlike.entity.CurationLikeId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * CurationLike 레포지토리
 * - Lock 순서: 사용자 > 큐레이션 > 좋아요
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


    /**
     * 사용자가 좋아요를 누른 큐레이션 id 목록 조회
     * - 잠글 큐레이션 후보를 고르는 용도. 이후 잠금 조회로 다시 확인한다
     * - 기본키 (user_id, curation_id)의 앞부분으로 찾는다
     */
    @Query("""
        SELECT l.id.curationId
        FROM CurationLike l
        WHERE l.id.userId = :userId
    """)
    List<UUID> findCurationIdsByUserId(@Param("userId") UUID userId);


    /**
     * 주어진 큐레이션들 중 사용자의 좋아요 행 배타 잠금 조회
     * - 호출 전제: 주어진 큐레이션 행들을 먼저 잠근 상태여야 한다 (Lock 순서: 사용자 > 큐레이션 > 좋아요)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l
        FROM CurationLike l
        WHERE l.id.userId = :userId
          AND l.id.curationId IN :curationIds
        ORDER BY l.id.curationId
    """)
    List<CurationLike> findAllByUserIdAndCurationIdInForUpdate(@Param("userId") UUID userId, @Param("curationIds") Collection<UUID> curationIds);


    /**
     * 주어진 큐레이션들에서 사용자의 좋아요 삭제
     * - 지운 행 수 반환
     * - 호출 전제: 지울 좋아요 행들을 먼저 잠금 조회한 상태여야 한다
     */
    @Modifying
    @Query("""
        DELETE FROM CurationLike l
        WHERE l.id.userId = :userId
          AND l.id.curationId IN :curationIds
    """)
    int deleteByUserIdAndCurationIdIn(@Param("userId") UUID userId, @Param("curationIds") Collection<UUID> curationIds);
}
