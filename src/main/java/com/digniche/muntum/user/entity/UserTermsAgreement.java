package com.digniche.muntum.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.digniche.muntum.common.entity.BaseEntity;
import java.util.HashMap;
import java.util.Objects;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(name = "user_terms_agreements",
        uniqueConstraints = {@UniqueConstraint(name = "uk_user_terms_user_version", columnNames = {"user_id", "version"})},
        indexes = {@Index(name = "idx_user_terms_user_id", columnList = "user_id")}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermsAgreement extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 서비스 이용약관 동의 시각 — 필수
     */
    @Column(name = "terms_of_service_at")
    private LocalDateTime termsOfServiceAt;

    /**
     * 기존 개인정보 처리방침 동의 시각 — 레거시 기록 보존용
     */
    @Column(name = "privacy_policy_at")
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "agreement_versions", columnDefinition = "json")
    private Map<String, String> agreementVersions;

    @Enumerated(EnumType.STRING)
    @Column(name = "signup_consent_source", length = 32)
    private SignupConsentSource signupConsentSource;

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
            String version,
            Map<String, String> agreementVersions,
            SignupConsentSource signupConsentSource
    ) {
        this.user = user;
        this.termsOfServiceAt = termsOfServiceAt;
        this.privacyPolicyAt = privacyPolicyAt;
        this.marketingPushAt = marketingPushAt;
        this.marketingEmailAt = marketingEmailAt;
        this.locationTermsAt = locationTermsAt;
        this.thirdPartyOfferAt = thirdPartyOfferAt;
        this.version = version;
        this.agreementVersions = agreementVersions;
        this.signupConsentSource = signupConsentSource;
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

    /**
     * 선택 약관의 현재 동의 시각을 반환한다.
     * 필수 약관과 개인정보처리방침 고지 문서는 제외한다.
     */
    public LocalDateTime optionalAcceptedAt(UserTermsType type) {
        return switch (type) {
            case MARKETING_PUSH -> marketingPushAt;
            case MARKETING_EMAIL -> marketingEmailAt;
            case LOCATION_TERMS -> locationTermsAt;
            case THIRD_PARTY_OFFER -> thirdPartyOfferAt;
            default -> throw new IllegalArgumentException(
                    "Not an optional agreement: " + type
            );
        };
    }
    /**
     * 구버전 앱 위치 접근 허용 호환 처리.
     * 실제 약관에 대한 명시적 동의로 간주하지 않는다.
     * TODO: 구버전 앱 지원 종료 후 제거
     */
    public void recordLegacyLocationPermission(
            LocalDateTime recordedAt
    ) {
        if (locationTermsAt != null) {
            return;
        }

        this.locationTermsAt = Objects.requireNonNull(recordedAt);

        Map<String, String> updatedVersions =
                new HashMap<>(
                        agreementVersions == null
                                ? Map.of()
                                : agreementVersions
                );

        updatedVersions.remove(UserTermsType.LOCATION_TERMS.name());

        this.agreementVersions = updatedVersions;
    }
    /**
     * 하나의 선택 약관 동의 상태 변경
     *
     * - 동의하려면 검증된 문서 버전이 필요
     * - 철회할 때는 활성화된 문서가 필요하지 않음
     * - 다른 약관 동의 값은 유지
     * - 변경 사항이 없는 동일한 요청은 아무것도 수정하지 않음
     */
    public void updateOptionalConsent(
            UserTermsType type,
            boolean agreed,
            String validatedVersion,
            LocalDateTime recordedAt
    ) {
        LocalDateTime previous = optionalAcceptedAt(type);

        if (!agreed) {
            // 현재 활성화된 동의가 없다면 철회할 것도 없음
            if (previous == null) {
                return;
            }

            setOptionalAcceptedAt(type, null);

            // 기존에 저장된 문서 버전 이력은 유지. 레거시 기록에 존재하지 않는 버전을 임의로 만들지 않음
            return;
        }

        Objects.requireNonNull(
                validatedVersion,
                "Validated version is required for acceptance"
        );
        Objects.requireNonNull(recordedAt);

        String previousVersion =
                agreementVersions == null
                        ? null
                        : agreementVersions.get(type.name());

        // 이미 동의한 상태이고 문서 버전도 같다면 기존 동의 시각을 유지
        if (previous != null
                && validatedVersion.equals(previousVersion)) {
            return;
        }

        setOptionalAcceptedAt(type, recordedAt);

        Map<String, String> updatedVersions =
                new HashMap<>(
                        agreementVersions == null
                                ? Map.of()
                                : agreementVersions
                );

        updatedVersions.put(type.name(), validatedVersion);
        this.agreementVersions = updatedVersions;
    }

    private void setOptionalAcceptedAt(
            UserTermsType type,
            LocalDateTime timestamp
    ) {
        switch (type) {
            case MARKETING_PUSH -> marketingPushAt = timestamp;
            case MARKETING_EMAIL -> marketingEmailAt = timestamp;
            case LOCATION_TERMS -> locationTermsAt = timestamp;
            case THIRD_PARTY_OFFER -> thirdPartyOfferAt = timestamp;
            default -> throw new IllegalArgumentException(
                    "Cannot change required terms or privacy notice"
            );
        }
    }
}