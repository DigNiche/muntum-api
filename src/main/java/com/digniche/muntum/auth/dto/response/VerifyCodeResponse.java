package com.digniche.muntum.auth.dto.response;

/**
 * 비밀번호 찾기 - 인증번호 확인 응답 DTO
 */
public record VerifyCodeResponse(
        String resetToken
) {}
