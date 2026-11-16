package com.digniche.muntum.auth.dto.request;

import com.digniche.muntum.user.entity.UserTermsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupConsentChoice(

        @NotNull
        UserTermsType termType,

        @NotBlank
        String version,

        @NotNull
        Boolean agreed

) {
}