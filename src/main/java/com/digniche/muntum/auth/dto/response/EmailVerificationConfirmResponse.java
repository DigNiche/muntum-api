package com.digniche.muntum.auth.dto.response;

/**
 * 회원가입 - 이메일 인증번호 확인 응답 DTO
 */
public record EmailVerificationConfirmResponse(
        String signupToken
) {}