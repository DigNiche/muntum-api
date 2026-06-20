package com.digniche.muntum.global.security.jwt;


import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 프로바이더 - 토큰 생성, 검증, claims 파싱
 */
@Component
public class JwtProvider {
    private static final String CLAIM_ROLE = "role";
    private final SecretKey key;
    private final long accessTokenExpirationTime;
    // TODO: private final long refreshTokenExpirationTime;

    public JwtProvider(
            @Value("${jwt.secret-key}") String secret,
            @Value("${jwt.access-token-validity-time}") long accessTokenExpirationTime
            // TODO: @Value("${jwt.refresh-token-validity-time}") long refreshTokenExpirationTime
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        // TODO: this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    // Access 토큰 생성
    public String generateAccessToken(User user) {
        return buildToken(user);
    }

    private String buildToken(User user) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + accessTokenExpirationTime);

        JwtBuilder builder = Jwts.builder()
                .subject(user.getUserId().toString())
                .claim("role", user.getRole().name())
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key);
        if (user.getRole() != null) {
            builder.claim(CLAIM_ROLE, user.getRole().name());
        }
        return builder.compact();
    }

    // ---- 토큰 검증/파싱 (필터에서 사용) ----

    /** 서명·형식·만료 여부를 검증한다. 문제가 있으면 JwtException(하위 예외)이 던져진다. */
    public void validateToken(String token) {
        parseClaims(token); // 파싱(=서명/만료 검증)에 성공하면 끝, 결과는 버린다
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public UserRole getRole(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        return role != null ? UserRole.valueOf(role) : null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // TODO: 토큰 검증, 리프레시 토큰 검증 여부, 토큰 파싱, 리프레시 토큰 생성 (JwtTokenProviderImpl)

}
