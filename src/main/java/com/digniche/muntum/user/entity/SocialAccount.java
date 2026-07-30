package com.digniche.muntum.user.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_provider_subject",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(
            name = "provider_refresh_token",
            columnDefinition = "TEXT"
    )
    private String providerRefreshToken;

    @Builder
    public SocialAccount(
            User user,
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            String providerRefreshToken
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.providerRefreshToken=providerRefreshToken;
    }

    public void updateProviderRefreshToken(
            String providerRefreshToken
    ) {this.providerRefreshToken=providerRefreshToken;
    }
}