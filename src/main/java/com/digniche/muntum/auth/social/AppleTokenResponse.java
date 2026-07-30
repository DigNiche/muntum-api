package com.digniche.muntum.auth.social;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Apple /auth/token 응답
 */
public record AppleTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("id_token")
        String idToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType

) {
}