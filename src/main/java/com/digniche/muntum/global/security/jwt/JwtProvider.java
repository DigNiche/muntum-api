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
@Getter
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private final SecretKey key;
    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;

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
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + accessTokenExpirationTime);

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("tokenType", "ACCESS")  // ACCESS | REFRESH
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();
        log.debug("JWT 액세스 토큰 생성됨: 사용자 ID={}, 역할={}", user.getId(), user.getRole());

        return token;
    }

    // Refresh 토큰 생성
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + refreshTokenExpirationTime);

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("tokenType", "REFRESH")  // ACCESS | REFRESH
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();

        log.debug("JWT 리프레시 토큰 생성됨: 사용자 ID={}, 역할={}", user.getId(), user.getRole());

        return token;
    }


    /**
     * 토큰 파싱 / 검증
     */

    // 토큰 파싱
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰 검증
    public void validateToken(String token) {
        try {
            parseClaims(token); // 파싱 성공 후 결과 버림
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

    // Refresh Token 유효성 여부 확인
    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);

            // Refresh 토큰 타입 확인
            String tokenType = claims.get("tokenType", String.class);
            if (!"REFRESH".equals(tokenType)) {
                log.warn("Refresh Token 아닌 토큰으로 재발급 시도됨");
                return false;
            }
            return true;
        } catch(BusinessException e) {
            return false;
        }
    }

}
