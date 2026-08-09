package com.digniche.muntum.auth.dto.response;

/**
 * 회원가입 - 이메일 인증번호 발송 응답 DTO
 */
public record EmailVerificationSendResponse(
        long expiresIn,
        long resendAfter   // 재발송 가능해지기까지 남은 시간(쿨다운) (초)
) {}