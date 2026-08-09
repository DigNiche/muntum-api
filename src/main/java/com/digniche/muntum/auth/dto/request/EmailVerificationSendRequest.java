package com.digniche.muntum.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 - 이메일 인증번호 발송 요청 DTO
 */
public record EmailVerificationSendRequest(
        @NotBlank @Email String email
) {}