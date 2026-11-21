package com.digniche.muntum.suggestion.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "spot_suggestions", indexes = {
                @Index(name = "idx_suggestions_user_id", columnList = "informer"),
                @Index(name = "idx_suggestions_status", columnList = "status"),
                @Index(name = "idx_suggestions_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotSuggestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "informer")
    private UUID informer; // 제보자

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "program_name", nullable = false, length = 100)
    private String programName;

    @Column(name = "address", length = 255)
    private String address;

    @Lob
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Builder
    public SpotSuggestion(UUID informer, String programName, String address, String reason) {
        this.informer = informer;
        this.programName = programName;
        this.address = address;
        this.reason = reason;
        this.status = SuggestionStatus.PENDING;
    }
    // 논리 삭제: 제보자 목록에 노출 목적
    public void softDelete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void update(String programName, String address, String reason) {
        this.programName = programName;
        this.address = address;
        this.reason = reason;
    }

    public void startReviewBy(UUID reviewerId) {
        this.reviewedBy = reviewerId;
        this.status = SuggestionStatus.REVIEWING;
    }

    public void approveBy(UUID reviewerId) {
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.status = SuggestionStatus.APPROVED;
    }

    public void rejectBy(UUID reviewerId) {
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.status = SuggestionStatus.REJECTED;
    }
}

