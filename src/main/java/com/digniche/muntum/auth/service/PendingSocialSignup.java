package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.social.SocialUserInfo;
import com.digniche.muntum.user.entity.SocialProvider;

/**
 * Verified identity awaiting Muntum signup consent.
 * This is internal server-side data, not a Flutter DTO.
 */
public record PendingSocialSignup(
        SocialProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String encryptedAppleRefreshToken
) {
    public PendingSocialSignup {
        if (provider == null
                || providerUserId == null
                || providerUserId.isBlank()
                || email == null
                || email.isBlank()) {
            throw new IllegalArgumentException("Invalid pending social identity");
        }

        if (provider == SocialProvider.APPLE
                && (encryptedAppleRefreshToken == null
                || !encryptedAppleRefreshToken.startsWith("v1:"))) {
            throw new IllegalArgumentException("Encrypted Apple refresh token is required");
        }

        if (provider != SocialProvider.APPLE
                && encryptedAppleRefreshToken != null) {
            throw new IllegalArgumentException("Unexpected provider refresh token");
        }
    }

    public SocialUserInfo toSocialUserInfo() {
        return new SocialUserInfo(
                provider,
                providerUserId,
                email,
                emailVerified
        );
    }
}