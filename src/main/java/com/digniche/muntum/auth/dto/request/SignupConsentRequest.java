package com.digniche.muntum.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SignupConsentRequest(

        @NotEmpty
        List<@Valid SignupConsentChoice> agreements

) {
}