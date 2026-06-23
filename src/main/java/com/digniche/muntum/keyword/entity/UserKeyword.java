package com.digniche.muntum.keyword.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import com.digniche.muntum.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 이 Entity가 사용자와 키워드를 연결하지만, 기능 중심으로 보면 키워드 선택 관계에 더 가깝기 때문에 Keyword에다가 둠.
@Entity
@Table(
        name = "user_keywords",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_keywords_user_keyword",
                        columnNames = {"user_id", "keyword_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_keywords_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_user_keywords_keyword_id",
                        columnList = "keyword_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserKeyword extends BaseEntity {

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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Builder
    public UserKeyword(User user, Keyword keyword) {
        this.user = user;
        this.keyword = keyword;
    }
}