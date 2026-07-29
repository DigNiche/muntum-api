package com.digniche.muntum.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 회원가입 - 이메일 인증번호 확인 요청 DTO
 */
public record EmailVerificationConfirmRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "인증번호는 숫자 6자리입니다") String code
) {}