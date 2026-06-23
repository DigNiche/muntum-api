package com.digniche.muntum.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_terms_agreements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_terms_user_type_version",
                        columnNames = {"user_id", "terms_type", "version"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_terms_user_id",
                        columnList = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermsAgreement {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, length = 20)
    private TermsType termsType;

    @Column(name = "agreed", nullable = false)
    private boolean agreed = false;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Builder
    public UserTermsAgreement(
            User user,
            TermsType termsType,
            String version
    ) {
        this.user = user;
        this.termsType = termsType;
        this.version = version;
        this.agreed = false;
    }
    public void agree() {
        this.agreed = true;
        this.agreedAt = LocalDateTime.now();
        this.revokedAt = null;
    }

    public void revoke() {
        this.agreed = false;
        this.revokedAt = LocalDateTime.now();
    }
}