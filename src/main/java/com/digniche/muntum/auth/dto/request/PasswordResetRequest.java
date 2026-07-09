package com.digniche.muntum.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 요청 DTO
 */
public record PasswordResetRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String newPassword
) {}
