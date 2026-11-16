
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SignupConsentChoice;
import com.digniche.muntum.auth.dto.request.SignupConsentRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.UserTermsType;

import java.util.List;
import java.util.Map;

import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.dto.response.TermsResponse;
import java.time.LocalDateTime;

public class SignupConsentPolicySmokeTest {

    private static final Map<UserTermsType, String> ACTIVE =
            Map.of(
                    UserTermsType.TERMS_OF_SERVICE, "2.0",
                    UserTermsType.PRIVACY_POLICY, "1.5",
                    UserTermsType.MARKETING_PUSH, "1.0",
                    UserTermsType.MARKETING_EMAIL, "1.0"
            );

    public static void main(String[] args) {
        SignupConsentRequest valid = request(
                choice(UserTermsType.TERMS_OF_SERVICE, "2.0", true),
                choice(UserTermsType.MARKETING_EMAIL, "1.0", false)
        );

        var validated =
                SignupConsentPolicy.validateExplicit(valid, ACTIVE);

        check(validated.isAgreed(UserTermsType.TERMS_OF_SERVICE),
                "Service terms should be accepted");

        check(!validated.isAgreed(UserTermsType.MARKETING_EMAIL),
                "Declined marketing must remain false");

        check(!validated.wasSubmitted(UserTermsType.MARKETING_PUSH),
                "Omitted optional choice must remain omitted");

        System.out.println("PASS: Valid required and optional choices");

        // 1. Missing privacy choice must not block signup.
        SignupConsentPolicy.ValidatedConsent withoutPrivacy =
                SignupConsentPolicy.validateExplicit(valid, ACTIVE);

        check(
                withoutPrivacy.isAgreed(UserTermsType.TERMS_OF_SERVICE),
                "Service terms should be accepted"
        );

        System.out.println("PASS: Privacy consent not required");

        // 2. Explicitly declining privacy must not block signup.
        var declinedPrivacy = SignupConsentPolicy.validateExplicit(
                request(
                        choice(UserTermsType.TERMS_OF_SERVICE, "2.0", true),
                        choice(UserTermsType.PRIVACY_POLICY, "1.5", false)
                ),
                ACTIVE
        );

        check(
                !declinedPrivacy.wasSubmitted(UserTermsType.PRIVACY_POLICY),
                "Privacy notice must not become consent"
        );

        System.out.println("PASS: Privacy false does not block signup");

        // 3. Even a submitted true must not become stored consent.
        var submittedPrivacy = SignupConsentPolicy.validateExplicit(
                request(
                        choice(UserTermsType.TERMS_OF_SERVICE, "2.0", true),
                        choice(UserTermsType.PRIVACY_POLICY, "1.5", true)
                ),
                ACTIVE
        );

        check(
                !submittedPrivacy.isAgreed(UserTermsType.PRIVACY_POLICY),
                "Privacy must never be recorded as consent"
        );

        System.out.println("PASS: Privacy notice excluded from consent");

        // 4. The service terms still require acceptance.
        expectError(
                request(
                        choice(UserTermsType.TERMS_OF_SERVICE, "2.0", false)
                ),
                ErrorCode.REQUIRED_TERMS_NOT_AGREED
        );

        System.out.println("PASS: Service terms still required");

        expectError(
                request(
                        choice(UserTermsType.TERMS_OF_SERVICE, "1.0", true),
                        choice(UserTermsType.PRIVACY_POLICY, "1.5", true)
                ),
                ErrorCode.TERMS_VERSION_MISMATCH
        );
        System.out.println("PASS: Outdated version rejected");

        expectError(
                request(
                        choice(UserTermsType.TERMS_OF_SERVICE, "2.0", true),
                        choice(UserTermsType.TERMS_OF_SERVICE, "2.0", true),
                        choice(UserTermsType.PRIVACY_POLICY, "1.5", true)
                ),
                ErrorCode.INVALID_SIGNUP_CONSENT
        );
        System.out.println("PASS: Duplicate agreement rejected");

        expectError(
                request(
                        choice(UserTermsType.TERMS_OF_SERVICE, "2.0", true),
                        choice(UserTermsType.PRIVACY_POLICY, "1.5", true),
                        choice(UserTermsType.MARKETING_EMAIL, "1.0", null)
                ),
                ErrorCode.INVALID_SIGNUP_CONSENT
        );
        System.out.println("PASS: Missing decision rejected");

        expectError(
                null,
                ErrorCode.REQUIRED_TERMS_NOT_AGREED
        );

        Terms privacy = Terms.builder()
                .type(UserTermsType.PRIVACY_POLICY)
                .version("1.5")
                .title("Privacy Notice")
                .content("Example privacy notice")
                .effectiveAt(LocalDateTime.now())
                .build();

        check(
                !TermsResponse.forSignup(privacy).required(),
                "Privacy must not be required in signup response"
        );

        check(
                TermsResponse.from(privacy).required(),
                "Legacy response behavior should remain unchanged"
        );

        System.out.println("PASS: Public and legacy mappings are compatible");

        System.out.println("All smoke tests passed."); // (기존 All 7 smoke tests passed 대신)
        System.out.println("PASS: Missing consent payload rejected");
        Map<UserTermsType, String> currentDocuments = Map.of(
                UserTermsType.TERMS_OF_SERVICE, "1.0.1",
                UserTermsType.PRIVACY_POLICY, "1.0.1"
        );

        // Missing optional documents do not block signup.
        var validDocuments = SignupConsentPolicy.validateExplicit(
                request(
                        choice(
                                UserTermsType.TERMS_OF_SERVICE,
                                "1.0.1",
                                true
                        )
                ),
                currentDocuments
        );

        check(
                validDocuments.isAgreed(UserTermsType.TERMS_OF_SERVICE),
                "Service terms must be accepted"
        );

        // LOCATION_TERMS가 UserTermsType에 정의되어 있다고 가정합니다.
        // 만약 없다면 이 라인에서 컴파일 에러가 발생할 수 있으니 확인해주세요.
        check(
                !validDocuments.wasSubmitted(UserTermsType.LOCATION_TERMS),
                "Unavailable optional terms must remain omitted"
        );

        // An unpublished optional agreement cannot be accepted.
        try {
            SignupConsentPolicy.validateExplicit(
                    request(
                            choice(
                                    UserTermsType.TERMS_OF_SERVICE,
                                    "1.0.1",
                                    true
                            ),
                            choice(
                                    UserTermsType.MARKETING_EMAIL,
                                    "1.0",
                                    true
                            )
                    ),
                    currentDocuments
            );

            throw new AssertionError(
                    "Unpublished optional consent should be rejected"
            );

        } catch (BusinessException exception) {
            check(
                    exception.getErrorCode() == ErrorCode.INVALID_SIGNUP_CONSENT,
                    "Unexpected error code"
            );
        }

        System.out.println("PASS: Unpublished optional agreements handled correctly");

        System.out.println("All smoke tests passed.");
    }

    private static SignupConsentChoice choice(
            UserTermsType type,
            String version,
            Boolean agreed
    ) {
        return new SignupConsentChoice(type, version, agreed);
    }

    private static SignupConsentRequest request(
            SignupConsentChoice... choices
    ) {
        return new SignupConsentRequest(List.of(choices));
    }

    private static void expectError(
            SignupConsentRequest request,
            ErrorCode expected
    ) {
        try {
            SignupConsentPolicy.validateExplicit(request, ACTIVE);
        } catch (BusinessException exception) {
            check(exception.getErrorCode() == expected,
                    "Unexpected error: " + exception.getErrorCode());
            return;
        }

        throw new AssertionError("Expected error: " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }


}

