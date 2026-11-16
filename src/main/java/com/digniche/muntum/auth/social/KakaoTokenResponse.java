package com.digniche.muntum.auth.social;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse(
        Long id,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("app_id")
        Long appId
) {
}