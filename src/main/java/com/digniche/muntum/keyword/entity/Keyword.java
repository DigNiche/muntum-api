package com.digniche.muntum.keyword.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * 키워드 엔티티
 */
@Entity
@Table(name = "keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private KeywordType type;

    @Column(name = "categories", length = 255)
    private String categories;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    /**
     * 논리 삭제 처리
     */
    public void softDelete(UUID deletedBy) {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    @Builder
    public Keyword(
            String name,
            String description,
            KeywordType type,
            String categories
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.categories = categories;
        this.active = true;
    }

    public void activate() {
        this.active = true;
    }
    public void deactivate() {
        this.active = false;
    }

    public void update(String name, String description, KeywordType type, String categories) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.categories = categories;
    }
}

