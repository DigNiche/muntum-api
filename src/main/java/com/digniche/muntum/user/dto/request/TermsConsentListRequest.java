package com.digniche.muntum.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 사용자 약관 항목 동의/비동의 모음 Request
 * @param terms
 */
public record TermsConsentListRequest(
        @NotEmpty List<@Valid TermsConsentRequest> terms
) {}
