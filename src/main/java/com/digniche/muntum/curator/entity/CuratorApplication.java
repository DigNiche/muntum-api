package com.digniche.muntum.curator.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import com.digniche.muntum.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 큐레이터 지원서
 * - Audience 사용자가 Curator로 전환 신청하기 위해 필요한 지원서
 */
@Entity
@Table(name = "curator_applications", indexes = {
        @Index(name = "idx_curator_applications_applicant_id", columnList = "applicant_id"),
        @Index(name = "idx_curator_applications_status", columnList = "status"),
        @Index(name = "idx_curator_applications_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CuratorApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(name = "program_name", nullable = false, length = 100)
    private String programName;

    @Column(name = "tagline", nullable = false, length = 255)
    private String tagline;

    @Lob
    @Column(name = "curation", nullable = false, columnDefinition = "TEXT")
    private String curation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CuratorApplicationStatus status = CuratorApplicationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason", length = 50)
    private CuratorApplicationRejectReason rejectReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    public CuratorApplication(
            User applicant,
            String programName,
            String tagline,
            String curation
    ) {
        this.applicant = applicant;
        this.programName = programName;
        this.tagline = tagline;
        this.curation = curation;
        this.status = CuratorApplicationStatus.PENDING;
    }

    public void approve(User reviewer) {
        this.reviewedBy = reviewer.getId();
        this.reviewedAt = LocalDateTime.now();
        this.status = CuratorApplicationStatus.APPROVED;
    }

    public void reject(User reviewer, CuratorApplicationRejectReason reason) {
        this.reviewedBy = reviewer.getId();
        this.reviewedAt = LocalDateTime.now();
        this.status = CuratorApplicationStatus.REJECTED;
        this.rejectReason = reason;
    }
}