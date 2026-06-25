package com.digniche.muntum.suggestion.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import com.digniche.muntum.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "spot_suggestions",
        indexes = {
                @Index(
                        name = "idx_suggestions_user_id",
                        columnList = "informer"
                ),
                @Index(
                        name = "idx_suggestions_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_suggestions_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotSuggestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "informer", nullable = false) //제보한 사용자
    private User informer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by") // 검토한 관리자
    private User reviewedBy;

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

    @Builder
    public SpotSuggestion(
            User informer,
            String programName,
            String address,
            String reason
    ) {
        this.informer = informer;
        this.programName = programName;
        this.address = address;
        this.reason = reason;
        this.status = SuggestionStatus.PENDING;
    }
    //3
    public void startReview(User reviewer) {
        this.reviewedBy = reviewer;
        this.status = SuggestionStatus.REVIEWING;
    }

    public void approve(User reviewer) {
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.status = SuggestionStatus.APPROVED;
    }

    public void reject(User reviewer) {
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.status = SuggestionStatus.REJECTED;
    }
}

