package com.digniche.muntum.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.digniche.muntum.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_terms_agreements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_terms_user_version",
                        columnNames = {"user_id", "version"}
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
public class UserTermsAgreement extends BaseEntity{

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

    @Column(name = "agreed", nullable = false)
    private Boolean agreed = true;
    /**
     * 서비스 이용약관 동의 시각 — 필수
     */
    @Column(name = "terms_of_service_at", nullable = false)
    private LocalDateTime termsOfServiceAt;

    /**
     * 개인정보 수집 및 이용 동의 시각 — 필수
     */
    @Column(name = "privacy_policy_at", nullable = false)
    private LocalDateTime privacyPolicyAt;

    /**
     * 앱 푸시 알림 수신 동의 시각 — 선택
     */
    @Column(name = "marketing_push_at")
    private LocalDateTime marketingPushAt;

    /**
     * 이메일 광고·소식 수신 동의 시각 — 선택
     */
    @Column(name = "marketing_email_at")
    private LocalDateTime marketingEmailAt;

    /**
     * 위치기반 서비스 이용약관 동의 시각 — 선택
     */
    @Column(name = "location_terms_at")
    private LocalDateTime locationTermsAt;

    /**
     * 제3자 정보 제공 동의 시각 — 선택
     */
    @Column(name = "third_party_offer_at")
    private LocalDateTime thirdPartyOfferAt;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

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


    @Builder
    public UserTermsAgreement(
            User user,
            LocalDateTime termsOfServiceAt,
            LocalDateTime privacyPolicyAt,
            LocalDateTime marketingPushAt,
            LocalDateTime marketingEmailAt,
            LocalDateTime locationTermsAt,
            LocalDateTime thirdPartyOfferAt,
            String version
    ) {
        this.user = user;
        this.agreed = true;
        this.termsOfServiceAt = termsOfServiceAt;
        this.privacyPolicyAt = privacyPolicyAt;
        this.marketingPushAt = marketingPushAt;
        this.marketingEmailAt = marketingEmailAt;
        this.locationTermsAt = locationTermsAt;
        this.thirdPartyOfferAt = thirdPartyOfferAt;
        this.version = version;
    }

    public void agreeTerm(UserTermsType termType) {
        LocalDateTime now = LocalDateTime.now();
        switch (termType) {
            case TERMS_OF_SERVICE   -> this.termsOfServiceAt = now;
            case PRIVACY_POLICY     -> this.privacyPolicyAt = now;
            case MARKETING_PUSH     -> this.marketingPushAt = now;
            case MARKETING_EMAIL    -> this.marketingEmailAt = now;
            case LOCATION_TERMS     -> this.locationTermsAt = now;
            case THIRD_PARTY_OFFER  -> this.thirdPartyOfferAt = now;
        }
    }

    public boolean disagreeTerm(UserTermsType termType) {
        if (termType.isRequired()) {
            return false;
        }
        switch (termType) {
            case MARKETING_PUSH     -> this.marketingPushAt = null;
            case MARKETING_EMAIL    -> this.marketingEmailAt = null;
            case LOCATION_TERMS     -> this.locationTermsAt = null;
            case THIRD_PARTY_OFFER  -> this.thirdPartyOfferAt = null;
        }
        return true;
    }
}