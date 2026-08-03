package com.digniche.muntum.programreaction.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 사용자별 프로그램 좋아요·싫어요
 */
@Entity
@Table(
        name = "program_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_reaction_user_program",
                        columnNames = {"user_id", "program_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_program_reaction_user_type_updated",
                        columnList = "user_id, reaction_type, updated_at"
                ),
                @Index(
                        name = "idx_program_reaction_program_type",
                        columnList = "program_id, reaction_type"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramReaction extends BaseEntity {

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
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "program_id",
            nullable = false
    )
    private Program program;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "reaction_type",
            nullable = false,
            length = 10
    )
    private ReactionType reactionType;

    @Builder
    public ProgramReaction(
            User user,
            Program program,
            ReactionType reactionType
    ) {
        this.user = user;
        this.program = program;
        this.reactionType = reactionType;
    }

    /**
     * 좋아요 ↔ 싫어요 변경
     */
    public void changeType(ReactionType newType) {
        if (this.reactionType == newType) {
            return;
        }

        this.reactionType = newType;
    }
}