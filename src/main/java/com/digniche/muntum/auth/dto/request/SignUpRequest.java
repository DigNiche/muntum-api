package com.digniche.muntum.auth.dto.request;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 Req DTO
 */
public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String signupToken,
        // Existing legacy field
        String userTermsAgreementVersion,

        // New optional field for consent-aware signup
        @Valid SignupConsentRequest signupConsent
) {

    public SignUpRequest {
        if (userTermsAgreementVersion == null) {
            userTermsAgreementVersion = "1.0";
        }
    }

    // Preserve existing four-argument Java constructor
    public SignUpRequest(String email, String password, String signupToken, String userTermsAgreementVersion) {
        this(email, password, signupToken, userTermsAgreementVersion, null);
    }


    // TODO: Presentation 계층이 Domain 침범하지 않도록 추후 수정 예정
    public User toEntity(String encodedPassword, UserRole role) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build();
    }

}
