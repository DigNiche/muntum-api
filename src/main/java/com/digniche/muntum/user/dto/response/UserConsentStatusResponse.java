package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.UserTermsAgreement;
import com.digniche.muntum.user.entity.UserTermsType;

import java.time.LocalDateTime;
import java.util.Map;

public record UserConsentStatusResponse(
        Map<UserTermsType, Boolean> optionalConsents,
        Map<String, String> agreementVersions,
        LocalDateTime updatedAt
) {
    public static UserConsentStatusResponse from(
            UserTermsAgreement agreement
    ) {
        return new UserConsentStatusResponse(
                Map.of(
                        UserTermsType.MARKETING_PUSH,
                        agreement.getMarketingPushAt() != null,

                        UserTermsType.MARKETING_EMAIL,
                        agreement.getMarketingEmailAt() != null,

                        UserTermsType.LOCATION_TERMS,
                        agreement.getLocationTermsAt() != null,

                        UserTermsType.THIRD_PARTY_OFFER,
                        agreement.getThirdPartyOfferAt() != null
                ),
                agreement.getAgreementVersions() == null
                        ? Map.of()
                        : Map.copyOf(
                        agreement.getAgreementVersions()
                ),
                agreement.getUpdatedAt()
        );
    }
}