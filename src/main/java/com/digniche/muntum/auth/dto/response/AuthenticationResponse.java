package com.digniche.muntum.auth.dto.response;

import com.digniche.muntum.user.entity.User;

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
    public static AuthenticationResponse of(TokenResponse token, User user) {
        return new AuthenticationResponse(
                token.tokenType(), token.accessToken(), token.accessExpiresIn(),
                token.refreshToken(), token.refreshExpiresIn(),
                user.getId(), user.getEmail(), user.getNickname()
        );
    }
}