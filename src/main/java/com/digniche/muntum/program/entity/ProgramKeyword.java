package com.digniche.muntum.program.entity;

import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(
        name = "program_keywords",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_keywords_program_keyword",
                        columnNames = {"program_id", "keyword_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_program_keywords_program_id",
                        columnList = "program_id"
                ),
                @Index(
                        name = "idx_program_keywords_keyword_id",
                        columnList = "keyword_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramKeyword extends BaseEntity {

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
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Builder
    public ProgramKeyword(Program program, Keyword keyword) {
        this.program = program;
        this.keyword = keyword;
    }
}