
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SignupConsentChoice;
import com.digniche.muntum.auth.dto.request.SignupConsentRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.TermsRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

public class SignupConsentRecorderSmokeTest {

    public static void main(String[] args) {
        User user = User.createSocialUser(
                "test@example.com", true
        );

        Terms service = activeTerms(
                UserTermsType.TERMS_OF_SERVICE, "1.0.1"
        );
        Terms privacy = activeTerms(
                UserTermsType.PRIVACY_POLICY, "1.0.1"
        );

        SignupConsentRecorder recorder =
                createRecorder(List.of(service, privacy));

        // Test 1: Explicit signup, no optional documents.
        UserTermsAgreement explicit =
                recorder.saveExplicitSignup(
                        user,
                        request(
                                choice(
                                        UserTermsType.TERMS_OF_SERVICE,
                                        "1.0.1", true
                                )
                        )
                );

        check(explicit.getTermsOfServiceAt() != null,
                "Service terms timestamp missing");
        check(explicit.getPrivacyPolicyAt() == null,
                "Privacy notice must not record consent");
        check(explicit.getLocationTermsAt() == null,
                "Missing location consent must stay null");
        check(explicit.getAgreementVersions().isEmpty(),
                "No optional versions should be created");
        check(
                explicit.getSignupConsentSource()
                        == SignupConsentSource.EXPLICIT,
                "Expected EXPLICIT source"
        );

        System.out.println("PASS: Explicit signup");

        // Test 2: Legacy signup creates no consent evidence.
        UserTermsAgreement legacy =
                recorder.saveLegacyUnverifiedSignup(user);

        check(legacy.getTermsOfServiceAt() == null,
                "Legacy service consent must remain null");
        check(legacy.getPrivacyPolicyAt() == null,
                "Legacy privacy timestamp must remain null");
        check(
                legacy.getSignupConsentSource()
                        == SignupConsentSource.LEGACY_UNVERIFIED,
                "Expected legacy source"
        );

        System.out.println("PASS: Legacy signup");

        // Test 3: Unavailable optional agreement is rejected.
        try {
            recorder.saveExplicitSignup(
                    user,
                    request(
                            choice(
                                    UserTermsType.TERMS_OF_SERVICE,
                                    "1.0.1", true
                            ),
                            choice(
                                    UserTermsType.MARKETING_EMAIL,
                                    "1.0", true
                            )
                    )
            );

            throw new AssertionError(
                    "Unpublished marketing consent was accepted"
            );
        } catch (BusinessException exception) {
            check(
                    exception.getErrorCode()
                            == ErrorCode.INVALID_SIGNUP_CONSENT,
                    "Wrong error for unpublished document"
            );
        }

        System.out.println("PASS: Unavailable optional document");

        // Test 4: Optional acceptance and decline.
        Terms email = activeTerms(
                UserTermsType.MARKETING_EMAIL, "1.0"
        );
        Terms push = activeTerms(
                UserTermsType.MARKETING_PUSH, "1.1"
        );

        SignupConsentRecorder withOptional =
                createRecorder(List.of(
                        service, privacy, email, push
                ));

        UserTermsAgreement optional =
                withOptional.saveExplicitSignup(
                        user,
                        request(
                                choice(
                                        UserTermsType.TERMS_OF_SERVICE,
                                        "1.0.1", true
                                ),
                                choice(
                                        UserTermsType.MARKETING_EMAIL,
                                        "1.0", true
                                ),
                                choice(
                                        UserTermsType.MARKETING_PUSH,
                                        "1.1", false
                                )
                        )
                );

        check(optional.getMarketingEmailAt() != null,
                "Accepted email marketing should have a timestamp");
        check(optional.getMarketingPushAt() == null,
                "Declined push consent must remain null");
        check(
                "1.0".equals(optional.getAgreementVersions()
                        .get("MARKETING_EMAIL")),
                "Incorrect marketing email version"
        );
        check(
                "1.1".equals(optional.getAgreementVersions()
                        .get("MARKETING_PUSH")),
                "Declined choice should retain submitted version"
        );

        System.out.println("PASS: Optional choices");
        System.out.println("All 4 smoke tests passed.");
    }

    private static Terms activeTerms(
            UserTermsType type, String version
    ) {
        Terms terms = Terms.builder()
                .type(type)
                .version(version)
                .title(type.name())
                .content("Test document content")
                .effectiveAt(LocalDateTime.now().minusDays(1))
                .build();

        terms.activate();
        return terms;
    }

    private static SignupConsentChoice choice(
            UserTermsType type, String version, Boolean agreed
    ) {
        return new SignupConsentChoice(type, version, agreed);
    }

    private static SignupConsentRequest request(
            SignupConsentChoice... choices
    ) {
        return new SignupConsentRequest(List.of(choices));
    }

    private static SignupConsentRecorder createRecorder(
            List<Terms> documents
    ) {
        TermsRepository termsRepository =
                (TermsRepository) Proxy.newProxyInstance(
                        TermsRepository.class.getClassLoader(),
                        new Class<?>[]{TermsRepository.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals(
                                    "findAllByActiveTrueAndDeletedAtIsNull"
                            )) {
                                return documents;
                            }
                            throw new UnsupportedOperationException(
                                    method.getName()
                            );
                        }
                );

        UserTermsAgreementRepository agreementRepository =
                (UserTermsAgreementRepository)
                        Proxy.newProxyInstance(
                                UserTermsAgreementRepository.class
                                        .getClassLoader(),
                                new Class<?>[]{
                                        UserTermsAgreementRepository.class
                                },
                                (proxy, method, args) -> {
                                    if (method.getName().equals("save")) {
                                        return args[0];
                                    }
                                    throw new UnsupportedOperationException(
                                            method.getName()
                                    );
                                }
                        );

        return new SignupConsentRecorder(
                termsRepository, agreementRepository
        );
    }

    private static void check(
            boolean condition, String message
    ) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
