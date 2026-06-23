package com.digniche.muntum.auth.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원가입 Req DTO
 */
public record SignupResponse(
        UUID userId,
        String email,
        LocalDateTime createdAt
) {
    public static SignupResponse of(UUID userId, String email, LocalDateTime createdAt) {
        return new SignupResponse(userId, email, createdAt);
    }
}