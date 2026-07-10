package com.digniche.muntum.user.dto.request;

import com.digniche.muntum.user.entity.UserTermsType;
import jakarta.validation.constraints.NotNull;

/**
 * 약관 별 동의, 비동의 여부 Request
 */
public record TermsConsentRequest(
        @NotNull UserTermsType termType,
        boolean agreed
) {}
