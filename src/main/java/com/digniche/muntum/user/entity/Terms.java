package com.digniche.muntum.user.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 약관 원문 엔티티
 */
@Entity
@Table(name = "terms", uniqueConstraints = {@UniqueConstraint(name = "uk_terms_type_version", columnNames = {"type", "version"})},
        indexes = {@Index(name = "idx_terms_type", columnList = "type")}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Terms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    // 약관 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private UserTermsType type;

    // 버전 문자열(예: "1.0.13")
    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    // 시행일
    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    // 현재 게시 중인 버전 여부 (타입당 1개만)
    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    // 논리 삭제 처리
    public void softDelete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    @Builder
    public Terms(
            UserTermsType type,
            String version,
            String title,
            String content,
            LocalDateTime effectiveAt
    ) {
        this.type = type;
        this.version = version;
        this.title = title;
        this.content = content;
        this.effectiveAt = effectiveAt;
        this.active = false;   // 등록 시 비활성, 게시는 activate()로
    }


    // 약관 정보 수정 (게시 전 보완용)
    public void update(String title, String content, LocalDateTime effectiveAt) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (effectiveAt != null) this.effectiveAt = effectiveAt;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
