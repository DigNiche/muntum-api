package com.digniche.muntum.auth.dto;

/**
 * 인증 Res DTO
 */
public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken   // TODO: 추후 Refresh Token 활성화 시 사용
) {
    public static AuthenticationResponse of(String accessToken, long expiresInSeconds) {
        return new AuthenticationResponse(accessToken, "Bearer", expiresInSeconds, null);
    }
}
