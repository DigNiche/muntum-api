package com.digniche.muntum.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialSignupRequest(

        @NotBlank
        String signupTicket,

        @Valid
        @NotNull
        SignupConsentRequest signupConsent

) {
}