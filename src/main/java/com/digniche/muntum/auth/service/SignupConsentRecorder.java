
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SignupConsentRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.TermsRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;
import com.digniche.muntum.user.service.TermsAvailability;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SignupConsentRecorder {

    private final TermsRepository termsRepository;
    private final UserTermsAgreementRepository agreementRepository;

    private static final List<UserTermsType> OPTIONAL_TYPES = List.of(
            UserTermsType.MARKETING_PUSH,
            UserTermsType.MARKETING_EMAIL,
            UserTermsType.LOCATION_TERMS,
            UserTermsType.THIRD_PARTY_OFFER
    );

    /**
     * New signup with explicitly submitted consent.
     * Must participate in the caller's signup transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UserTermsAgreement saveExplicitSignup(User user, SignupConsentRequest request) {
        LocalDateTime recordedAt = LocalDateTime.now();

        // 1. Load currently available document versions.
        Map<UserTermsType, String> activeVersions = loadActiveVersions(recordedAt);

        // 2. Validate submitted decisions.
        SignupConsentPolicy.ValidatedConsent consent =
                SignupConsentPolicy.validateExplicit(request, activeVersions);

        // 3. Store versions only for submitted optional choices.
        Map<String, String> optionalVersions = new HashMap<>();

        for (UserTermsType type : OPTIONAL_TYPES) {
            if (consent.wasSubmitted(type)) {
                optionalVersions.put(type.name(), consent.versionFor(type));
            }
        }

        // 4. Create the current consent record.
        UserTermsAgreement agreement =
                UserTermsAgreement.builder()
                        .user(user)
                        .termsOfServiceAt(recordedAt)
                        .privacyPolicyAt(null)
                        .marketingPushAt(acceptedAt(consent, UserTermsType.MARKETING_PUSH, recordedAt))
                        .marketingEmailAt(acceptedAt(consent, UserTermsType.MARKETING_EMAIL, recordedAt))
                        .locationTermsAt(acceptedAt(consent, UserTermsType.LOCATION_TERMS, recordedAt))
                        .thirdPartyOfferAt(acceptedAt(consent, UserTermsType.THIRD_PARTY_OFFER, recordedAt))
                        .version(activeVersions.get(UserTermsType.TERMS_OF_SERVICE))
                        .agreementVersions(optionalVersions)
                        .signupConsentSource(SignupConsentSource.EXPLICIT)
                        .build();

        return agreementRepository.save(agreement);
    }

    /**
     * Temporary compatibility path.
     * No missing consent is treated as acceptance.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UserTermsAgreement saveLegacyUnverifiedSignup(User user) {
        Map<UserTermsType, String> activeVersions =
                loadActiveVersions(LocalDateTime.now());

        UserTermsAgreement agreement =
                UserTermsAgreement.builder()
                        .user(user)
                        .termsOfServiceAt(null)
                        .privacyPolicyAt(null)
                        .version(activeVersions.get(UserTermsType.TERMS_OF_SERVICE))
                        .agreementVersions(Map.of())
                        .signupConsentSource(SignupConsentSource.LEGACY_UNVERIFIED)
                        .build();

        return agreementRepository.save(agreement);
    }

    private LocalDateTime acceptedAt(
            SignupConsentPolicy.ValidatedConsent consent,
            UserTermsType type,
            LocalDateTime recordedAt
    ) {
        return consent.isAgreed(type) ? recordedAt : null;
    }

    /**
     * Only published, effective, non-empty documents
     * may be used for new consent collection.
     */
    private Map<UserTermsType, String> loadActiveVersions(LocalDateTime now) {
        Map<UserTermsType, String> versions = new EnumMap<>(UserTermsType.class);

        for (Terms terms :
                termsRepository.findAllByActiveTrueAndDeletedAtIsNull()) {

            if (!TermsAvailability.canCollectConsent(terms, now)) {
                continue;
            }

            if (versions.putIfAbsent(terms.getType(), terms.getVersion()) != null) {
                throw new BusinessException(ErrorCode.ACTIVE_TERMS_NOT_FOUND);
            }
        }

        // Service terms and privacy notice must exist.
        // Only service terms require affirmative consent.
        if (!versions.containsKey(UserTermsType.TERMS_OF_SERVICE)
                || !versions.containsKey(UserTermsType.PRIVACY_POLICY)) {
            throw new BusinessException(ErrorCode.ACTIVE_TERMS_NOT_FOUND);
        }

        return versions;
    }
}
