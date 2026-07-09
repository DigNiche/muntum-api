package com.digniche.muntum.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 찾기 - 인증번호 발송 요청 DTO
 */
public record PasswordFindRequest(
        @NotBlank @Email String email
) {}
