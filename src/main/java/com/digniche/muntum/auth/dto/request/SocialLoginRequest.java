package com.digniche.muntum.auth.dto.request;

import com.digniche.muntum.user.entity.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 소셜 로그인 요청 DTO
 */
public record SocialLoginRequest(

        @NotNull
        SocialProvider provider,

        @NotBlank
        String token,

        String authorizationCode,

        String nonce

) {
}