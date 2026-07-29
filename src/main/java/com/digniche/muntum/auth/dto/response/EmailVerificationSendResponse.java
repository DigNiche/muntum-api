package com.digniche.muntum.auth.dto.response;

/**
 * 회원가입 - 이메일 인증번호 발송 응답 DTO
 */
public record EmailVerificationSendResponse(
        long expiresIn
) {}