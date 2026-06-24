package com.digniche.muntum.scrap.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "scraps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_scrap_user_program",
                        columnNames = {"user_id", "program_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_scrap_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_scrap_program_id",
                        columnList = "program_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scraps extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    /**
     * 논리 삭제 처리
     */
    public void softDelete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Builder
    public Scraps(User user, Program program) {
        this.user = user;
        this.program = program;
    }
}