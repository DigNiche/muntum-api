package com.digniche.muntum.user.dto;

import com.digniche.muntum.user.entity.UserTermsType;
import jakarta.validation.constraints.NotNull;

/**
 * 약관 별 동의, 비동의 여부 Request
 * @param termType
 */
public record TermsConsentRequest(
        @NotNull UserTermsType termType,
        boolean agreed
) {}
