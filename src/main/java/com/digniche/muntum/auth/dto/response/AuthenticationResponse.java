package com.digniche.muntum.auth.dto.response;

import java.util.UUID;

/**
 * 인증 Res DTO
 */
public record AuthenticationResponse(
        String tokenType,
        String accessToken,
        long accessExpiresIn,
        String refreshToken,
        long refreshExpiresIn,
        UUID userId,
        String email,
        String nickname

) {
    public static AuthenticationResponse of(
            String accessToken, long accessExpiresIn,
            String refreshToken, long refreshExpiresIn,
            UUID userId, String email, String nickname) {
        return new AuthenticationResponse(
                "Bearer", accessToken, accessExpiresIn,
                refreshToken, refreshExpiresIn,
                userId, email, nickname
        );
    }
}