package com.digniche.muntum.auth.dto.response;

import java.util.UUID;

/**
 * 인증 Res DTO
 */
public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        long accessExpiresIn,
        String refreshToken,   // TODO: 추후 Refresh Token 활성화 시 사용
        long refreshExpiresIn,
        UUID userId,
        String email,
        String nickname

) {
    public static AuthenticationResponse of(String accessToken, long accessExpiresIn, String refreshToken, long refreshExpiresIn, UUID userId, String email, String nickname) {
        return new AuthenticationResponse(accessToken, "Bearer", accessExpiresIn, null, refreshExpiresIn, userId, email, nickname);
    }
}