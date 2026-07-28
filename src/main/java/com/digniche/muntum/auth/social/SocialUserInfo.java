package com.digniche.muntum.auth.social;

import com.digniche.muntum.user.entity.SocialProvider;

/**
 * 소셜 공급자 토큰 검증 결과
 */
public record SocialUserInfo(

        SocialProvider provider,

        String providerUserId,

        String email,

        boolean emailVerified

) {
}