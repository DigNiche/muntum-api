package com.digniche.muntum.global.security;

import com.digniche.muntum.user.entity.UserRole;

import java.util.UUID;

/**
 * 인증 Principal 인터페이스
 * - CustomUserDetails이 구현
 * - JwtUserPrincipal이 구현
 */
public interface AuthenticatedUser {
    UUID getUserId();
    UserRole getUserRole();
}
