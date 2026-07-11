package com.digniche.muntum.auth.dto.response;

/**
 * 비밀번호 찾기 - 인증번호 발송 응답 DTO
 */
public record PasswordFindResponse(
        long expiresIn   // 인증번호 유효 시간 (초)
) {}