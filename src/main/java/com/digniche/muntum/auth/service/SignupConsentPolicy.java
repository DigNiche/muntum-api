package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SignupConsentChoice;
import com.digniche.muntum.auth.dto.request.SignupConsentRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.UserTermsType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SignupConsentPolicy {

    private SignupConsentPolicy() {
    }

    public static ValidatedConsent validateExplicit(
            SignupConsentRequest request,
            Map<UserTermsType, String> activeVersions
    ) {
        Objects.requireNonNull(
                activeVersions,
                "activeVersions must not be null"
        );

        // Both documents must be published.
        // Only the service terms require affirmative acceptance.
        for (UserTermsType type : new UserTermsType[] {
                UserTermsType.TERMS_OF_SERVICE,
                UserTermsType.PRIVACY_POLICY
        }) {
            String version = activeVersions.get(type);

            if (version == null || version.isBlank()) {
                throw new BusinessException(
                        ErrorCode.ACTIVE_TERMS_NOT_FOUND
                );
            }
        }

        // 2. Require a consent payload for explicit signup.
        if (request == null
                || request.agreements() == null
                || request.agreements().isEmpty()) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        Map<UserTermsType, SignupConsentChoice> choices =
                new EnumMap<>(UserTermsType.class);

        // 3. Validate each submitted decision.
        for (SignupConsentChoice choice : request.agreements()) {

            if (choice == null
                    || choice.termType() == null
                    || choice.agreed() == null
                    || choice.version() == null
                    || choice.version().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_SIGNUP_CONSENT);
            }

            UserTermsType type = choice.termType();

            // Reject duplicate agreement types.
            if (choices.containsKey(type)) {
                throw new BusinessException(ErrorCode.INVALID_SIGNUP_CONSENT);
            }

            String activeVersion = activeVersions.get(type);

            // A submitted agreement must be published.
            if (activeVersion == null) {
                throw new BusinessException(ErrorCode.INVALID_SIGNUP_CONSENT);
            }

            // Prevent acceptance of outdated documents.
            if (!activeVersion.equals(choice.version())) {
                throw new BusinessException(ErrorCode.TERMS_VERSION_MISMATCH);
            }

            choices.put(type, choice);
        }

        // 4. Required agreements must explicitly be true.
        for (UserTermsType type : UserTermsType.values()) {

            if (!type.requiresSignupConsent()) {
                continue;
            }

            SignupConsentChoice choice = choices.get(type);

            if (choice == null
                    || !Boolean.TRUE.equals(choice.agreed())) {
                throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }
        }
        // A privacy notice is not an affirmative consent.
        // Never pass it to consent persistence.
        choices.remove(UserTermsType.PRIVACY_POLICY);

        return new ValidatedConsent(choices);
    }

    public record ValidatedConsent(
            Map<UserTermsType, SignupConsentChoice> choices
    ) {
        public ValidatedConsent {
            choices = Map.copyOf(choices);
        }

        public boolean wasSubmitted(UserTermsType type) {
            return choices.containsKey(type);
        }

        public boolean isAgreed(UserTermsType type) {
            SignupConsentChoice choice = choices.get(type);

            return choice != null
                    && Boolean.TRUE.equals(choice.agreed());
        }

        public String versionFor(UserTermsType type) {
            SignupConsentChoice choice = choices.get(type);

            return choice == null ? null : choice.version();
        }
    }
}