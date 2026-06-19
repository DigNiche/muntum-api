package com.digniche.muntum.global.security.jwt;


import com.digniche.muntum.user.entity.User;
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
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + accessTokenExpirationTime);

        return Jwts.builder()
                .subject(user.getUserId().toString())
                .claim("role", user.getRole().name())
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    // TODO: 토큰 검증, 리프레시 토큰 검증 여부, 토큰 파싱, 리프레시 토큰 생성 (JwtTokenProviderImpl)

}
