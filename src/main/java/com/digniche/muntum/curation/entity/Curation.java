package com.digniche.muntum.curation.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.entity.Program;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "curations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_curations_program_curator",
                        columnNames = {"program_id", "curator_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_program_curations_curator_created",
                        columnList = "curator_id, created_at"
                ),
                @Index(
                        name = "idx_program_curations_status_created",
                        columnList = "status, created_at"
                ),
                @Index(
                        name = "idx_program_curations_program_status",
                        columnList = "program_id, status"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Curation extends BaseEntity {

    public static final int MIN_IMAGE_COUNT = 1;
    public static final int MAX_IMAGE_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    /*
     * 승인 전에는 null.
     * 승인 시 기존 또는 신규 프로그램과 연결.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "program_id",
            foreignKey = @ForeignKey(
                    name = "fk_program_curations_program"
            )
    )
    private Program program;

    @Column(
            name = "curator_id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID curatorId;

    @Column(
            name = "submitted_program_title",
            nullable = false,
            length = 100
    )
    private String submittedProgramTitle;

    @Column(
            name = "submitted_place",
            nullable = false,
            length = 255
    )
    private String submittedPlace;

    @Column(
            name = "tagline",
            nullable = false,
            length = 255
    )
    private String tagline;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private CurationStatus status = CurationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "publication_status",
            nullable = false,
            length = 20
    )
    private CurationPublicationStatus publicationStatus
            = CurationPublicationStatus.UNPUBLISHED;

    @Column(
            name = "change_request_reason",
            length = 1000
    )
    private String changeRequestReason;

    @Column(
            name = "reviewed_by",
            columnDefinition = "BINARY(16)"
    )
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    public Curation(
            UUID curatorId,
            String submittedProgramTitle,
            String submittedPlace,
            String tagline,
            String content
    ) {
        this.curatorId = curatorId;
        this.submittedProgramTitle = submittedProgramTitle;
        this.submittedPlace = submittedPlace;
        this.tagline = tagline;
        this.content = content;
        this.status = CurationStatus.PENDING;
        this.publicationStatus =
                CurationPublicationStatus.UNPUBLISHED;
    }

    public void approve(
            Program program,
            UUID reviewerId
    ) {
        this.program = program;
        this.status = CurationStatus.APPROVED;
        this.publicationStatus =
                CurationPublicationStatus.PUBLISHED;
        this.changeRequestReason = null;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void requestChanges(
            UUID reviewerId,
            String changeRequestReason,
            CurationPublicationStatus requestedPublicationStatus
    ) {
        boolean wasPending =
                this.status == CurationStatus.PENDING;

        this.status = CurationStatus.CHANGES_REQUESTED;

        this.publicationStatus = wasPending
                ? CurationPublicationStatus.UNPUBLISHED
                : requestedPublicationStatus;

        this.changeRequestReason = changeRequestReason;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void updateContent(
            String submittedProgramTitle,
            String submittedPlace,
            String tagline,
            String content
    ) {
        this.submittedProgramTitle = submittedProgramTitle;
        this.submittedPlace = submittedPlace;
        this.tagline = tagline;
        this.content = content;
    }

    public void resubmit() {
        if (this.status != CurationStatus.CHANGES_REQUESTED) {
            throw new BusinessException(
                    ErrorCode.CURATION_NOT_RESUBMITTABLE
            );
        }

        this.status = CurationStatus.PENDING;
        this.reviewedBy = null;
        this.reviewedAt = null;
    }

    public void unpublish() {
        if (this.status != CurationStatus.APPROVED
                || this.publicationStatus != CurationPublicationStatus.PUBLISHED) {
            throw new BusinessException(
                    ErrorCode.CURATION_NOT_UNPUBLISHABLE
            );
        }

        this.publicationStatus =
                CurationPublicationStatus.UNPUBLISHED;
    }
}