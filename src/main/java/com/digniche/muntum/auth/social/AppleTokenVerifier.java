package com.digniche.muntum.auth.social;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.SocialProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Apple Identity Token 검증
 */
@Component
public class AppleTokenVerifier implements SocialTokenVerifier {

    private static final String APPLE_ISSUER =
            "https://appleid.apple.com";

    private static final String APPLE_JWK_SET_URI =
            "https://appleid.apple.com/auth/keys";

    private final JwtDecoder jwtDecoder;

    public AppleTokenVerifier(
            @Value("${social.apple.client-id}") String clientId
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(APPLE_JWK_SET_URI)
                .build();

        /*
         * iss, exp, nbf 등 기본 검증
         */
        OAuth2TokenValidator<Jwt> defaultValidator =
                JwtValidators.createDefaultWithIssuer(
                        APPLE_ISSUER
                );

        /*
         * aud가 문틈 Bundle ID인지 검증
         */
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience().contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Apple token audience does not match",
                    null
            );

            return OAuth2TokenValidatorResult.failure(error);
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        defaultValidator,
                        audienceValidator
                )
        );

        this.jwtDecoder = decoder;
    }

    @Override
    public SocialProvider supports() {
        return SocialProvider.APPLE;
    }

    @Override
    public SocialUserInfo verify(
            SocialLoginRequest request
    ) {
        Jwt jwt;

        try {
            jwt = jwtDecoder.decode(request.token());
        } catch (JwtException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }

        String providerUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        if (providerUserId == null
                || providerUserId.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }

        validateNonce(jwt, request.nonce());

        boolean emailVerified =
                parseBooleanClaim(
                        jwt.getClaim("email_verified")
                );

        return new SocialUserInfo(
                SocialProvider.APPLE,
                providerUserId,
                email,
                emailVerified
        );
    }

    /**
     * 요청에 nonce가 포함됐다면
     * Apple Identity Token의 nonce와 비교
     */
    private void validateNonce(
            Jwt jwt,
            String requestedNonce
    ) {
        if (requestedNonce == null
                || requestedNonce.isBlank()) {
            return;
        }

        String tokenNonce =
                jwt.getClaimAsString("nonce");

        if (tokenNonce == null
                || !secureEquals(tokenNonce, requestedNonce)) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }
    }

    private boolean parseBooleanClaim(Object claim) {
        if (claim instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return Boolean.parseBoolean(
                String.valueOf(claim)
        );
    }

    private boolean secureEquals(
            String first,
            String second
    ) {
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8)
        );
    }
}