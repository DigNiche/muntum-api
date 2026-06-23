package com.digniche.muntum.global.security.jwt;


import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 프로바이더 - 토큰 생성, 검증, claims 파싱
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    @Getter private final long accessTokenExpirationTime;
    @Getter private final long refreshTokenExpirationTime;

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_TOKEN_TYPE = "tokenType";

    private enum TokenType {
        ACCESS, REFRESH
    }

    public JwtProvider(
            @Value("${jwt.secret-key}") String secret,
            @Value("${jwt.access-token-validity-time}") long accessTokenExpirationTime,
            @Value("${jwt.refresh-token-validity-time}") long refreshTokenExpirationTime
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }


    /**
     * 토큰 생성
     * - Access 토큰 생성
     * - Refresh 토큰 생성
     */

    // Access 토큰 생성
    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpirationTime, TokenType.ACCESS);
    }

    // Refresh 토큰 생성
    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpirationTime, TokenType.REFRESH);
    }

    private String buildToken(User user, long expirationMillis, TokenType type) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expirationMillis);

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TOKEN_TYPE, type.name())
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();

        log.debug("JWT {} 토큰 생성됨: 사용자 ID={}, 역할={}", type, user.getId(), user.getRole());
        return token;
    }


    /**
     * 토큰 파싱 / 검증
     */

    // 토큰 파싱
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰 검증
    public Claims validateToken(String token) {
        try {
            return parseClaims(token);     // 토큰 파싱
        } catch (ExpiredJwtException e) {
            log.warn("🚫 JWT 토큰 만료: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("❌ JWT 토큰 잘못됨: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("❗ JWT 서명 불일치: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    // Refresh Token인지 확인
    public void validRefreshToken(String token) {
        Claims claims = validateToken(token);  // 토큰 파싱 및 검증

        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TokenType.REFRESH.name().equals(tokenType)) {
            log.warn("Refresh Token 아닌 토큰으로 재발급 시도됨");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

}
