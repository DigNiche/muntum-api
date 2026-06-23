package com.digniche.muntum.auth.dto.response;

import java.util.UUID;

/**
 * 인증 Res DTO
 */
public record AuthenticationResponse(
        String authenticationSchemePrefix,
        String accessToken,
        long accessExpiresIn,
        String refreshToken,
        long refreshExpiresIn,
        UUID userId,
        String email,
        String nickname

) {
    public static AuthenticationResponse of(String prefix, String accessToken, long accessExpiresIn, String refreshToken, long refreshExpiresIn, UUID userId, String email, String nickname) {
        return new AuthenticationResponse(
                prefix, accessToken, accessExpiresIn,
                refreshToken, refreshExpiresIn,
                userId, email, nickname
        );
    }
}