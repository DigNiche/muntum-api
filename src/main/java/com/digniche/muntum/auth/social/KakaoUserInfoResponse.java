package com.digniche.muntum.auth.social;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(

        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount

) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(

            String email,

            @JsonProperty("is_email_valid")
            Boolean emailValid,

            @JsonProperty("is_email_verified")
            Boolean emailVerified,

            @JsonProperty("email_needs_agreement")
            Boolean emailNeedsAgreement

    ) {
    }
}