package com.digniche.muntum.auth.dto.response;

/**
 * 토큰 Res DTO
 */
public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessExpiresIn,
        String refreshToken,
        long refreshExpiresIn
) {
    public static TokenResponse of(
            String accessToken, long accessExpiresIn,
            String refreshToken, long refreshExpiresIn) {
        return new TokenResponse(
                "Bearer", accessToken, accessExpiresIn,
                refreshToken, refreshExpiresIn
        );
    }
}
