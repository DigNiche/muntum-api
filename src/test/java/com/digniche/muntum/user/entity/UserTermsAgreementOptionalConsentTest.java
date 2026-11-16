package com.digniche.muntum.user.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserTermsAgreementOptionalConsentTest {

    @Test
    void historicalLocationConsentCanBeWithdrawn() {
        LocalDateTime historical =
                LocalDateTime.of(2026, 9, 10, 14, 30);

        UserTermsAgreement agreement =
                UserTermsAgreement.builder()
                        .version("1.0.1")
                        .privacyPolicyAt(historical)
                        .locationTermsAt(historical)
                        .marketingPushAt(historical)
                        .signupConsentSource(
                                SignupConsentSource.LEGACY_UNVERIFIED
                        )
                        .build();

        agreement.updateOptionalConsent(
                UserTermsType.LOCATION_TERMS,
                false,
                null,
                LocalDateTime.now()
        );

        assertNull(agreement.getLocationTermsAt());

        // 관계없는 다른 값들은 변경되지 않아야 한다.
        assertEquals(
                historical,
                agreement.getMarketingPushAt()
        );
        assertEquals(
                historical,
                agreement.getPrivacyPolicyAt()
        );
        assertEquals("1.0.1", agreement.getVersion());
        assertNull(agreement.getAgreementVersions());
    }

    @Test
    void acceptedOptionalConsentStoresItsDocumentVersion() {
        UserTermsAgreement agreement =
                UserTermsAgreement.builder()
                        .version("1.0.1")
                        .agreementVersions(Map.of())
                        .build();

        LocalDateTime recordedAt = LocalDateTime.now();

        // 호출하는 쪽에서 이 버전을 먼저 검증해야 한다.
        agreement.updateOptionalConsent(
                UserTermsType.MARKETING_EMAIL,
                true,
                "1.0",
                recordedAt
        );

        assertEquals(
                recordedAt,
                agreement.getMarketingEmailAt()
        );

        assertEquals(
                "1.0",
                agreement.getAgreementVersions()
                        .get("MARKETING_EMAIL")
        );

        // 변경 없는 동일한 동의를 반복해도
        // 최초 동의 시각이 초기화되지 않아야 한다.
        agreement.updateOptionalConsent(
                UserTermsType.MARKETING_EMAIL,
                true,
                "1.0",
                recordedAt.plusMinutes(10)
        );

        assertEquals(
                recordedAt,
                agreement.getMarketingEmailAt()
        );
    }

    @Test
    void requiredTermsAndPrivacyCannotBeChanged() {
        UserTermsAgreement agreement =
                UserTermsAgreement.builder()
                        .version("1.0.1")
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> agreement.updateOptionalConsent(
                        UserTermsType.PRIVACY_POLICY,
                        true,
                        "1.0.1",
                        LocalDateTime.now()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> agreement.updateOptionalConsent(
                        UserTermsType.TERMS_OF_SERVICE,
                        false,
                        null,
                        LocalDateTime.now()
                )
        );
    }
}