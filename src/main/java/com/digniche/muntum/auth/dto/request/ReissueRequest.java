package com.digniche.muntum.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token 재발급 Request
 */
public record ReissueRequest(
        @NotBlank String refreshToken
) {}
