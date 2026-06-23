package com.digniche.muntum.global.security;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * - DB에서 User 조회하여 UserDetails 객체로 반환
 * - user.getRole().toAuthority() -> "ROLD_AUDEINCE" (Ex)
 * - AuthenticationManager가 내부적으로 이 서비스를 사용해 authenticat() 수행
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(UUID userId, UserRole role) {
        this.user = User.ofClaims(userId, role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        user.getRole().toAuthority()
                )
        );
    }

    // 사용자 ID 조회
    public UUID getUserId() { return user.getId(); }

    // 사용자 Nickname 조회
    public String getNickname() { return user.getNickname(); }

    // 사용자 비밀번호 조회
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 사용자 이메일 조회 (Username 아님)
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    /*

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }
    */
}
