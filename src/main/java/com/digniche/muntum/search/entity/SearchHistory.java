package com.digniche.muntum.search.entity;

import com.digniche.muntum.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.digniche.muntum.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;
//4
@Entity
@Table(
        name = "search_histories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_search_histories_user_query",
                        columnNames = {"user_id", "query"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_search_histories_user_searched",
                        columnList = "user_id, last_searched_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistory extends BaseEntity {

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

    @Column(name = "query", nullable = false, length = 100)
    private String query;

    @Column(name = "last_searched_at", nullable = false)
    private LocalDateTime lastSearchedAt;

    @Builder
    public SearchHistory(User user, String query) {
        this.user = user;
        this.query = query;
        this.lastSearchedAt = LocalDateTime.now();
    }

    public void updateLastSearchedAt() {
        this.lastSearchedAt = LocalDateTime.now();
    }
}