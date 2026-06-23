package com.digniche.muntum.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 Req DTO
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
