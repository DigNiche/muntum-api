package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.response.TokenResponse;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.global.security.jwt.JwtProvider;
import com.digniche.muntum.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Access/Refresh 토큰 발급
 * 로그인, 토큰 재발급, 소셜 연동 로그인, 일반 회원 가입이 공유한다.
 */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    // 토큰 생성 + Refresh 토큰 Redis 저장
    public TokenResponse issue(User user) {
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        refreshTokenService.save(user.getId(), refreshToken, jwtProvider.getRefreshTokenExpirationTime());

        return TokenResponse.of(
                accessToken, jwtProvider.getAccessTokenExpirationTime(),
                refreshToken, jwtProvider.getRefreshTokenExpirationTime()
        );
    }
}
