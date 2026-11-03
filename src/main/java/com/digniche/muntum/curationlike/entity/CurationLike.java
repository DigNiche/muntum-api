package com.digniche.muntum.curationlike.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 큐레이션 좋아요
 * - 객체 아닌, 사람과 큐레이션 사이의 상태를 나타내므로, 별도 id 없이 복합 키로 둔다
 * - 연관관계(@ManyToOne) 없이 id만 둔다. 무결성은 마이그레이션의 FK가 지킨다
 * - 행은 저장소 쿼리(insertIfAbsent, deleteByUserIdAndCurationId)로만 만들고 지운다
 */
@Entity
@Table(name="curation_likes", indexes={@Index(name = "idx_curation_likes_curation_created_user", columnList = "curation_id, created_at, user_id")})
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class CurationLike {

    @EmbeddedId
    private CurationLikeId id;

    // BaseEntity 상속하지 않고 별도의 createdAt 필드 사용
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;
}
