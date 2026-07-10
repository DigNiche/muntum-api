package com.digniche.muntum.user.dto.request;

/**
 * 사용자 약관 동의/비동의 Request
 */
public record TermsAgreementRequest(
//        String version,
//        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다") // false: 400 Error
//        boolean termsOfService,
//
//        @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다")
//        boolean privacyPolicy,

        boolean marketingPush,
        boolean marketingEmail,
        boolean locationTerms,
        boolean thirdPartyOffer
) {}
