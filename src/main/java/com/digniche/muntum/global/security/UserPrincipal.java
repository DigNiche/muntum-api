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
 * 인증 주체(Principal) 어댑터
 * - UserDetailsService가 DB에서 User 조회 후 생성한 객체
 * - 로그인 시, 사용자 입력값과 비교하여 인증
 * - 계정 상태 변경 및 처리
 */
public class UserPrincipal implements UserDetails, AuthenticatedUser {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public UserPrincipal(UUID userId, String userRole) {
        this.user = User.ofClaims(userId, UserRole.valueOf(userRole));
    }

    /**
     * 사용자 정보 조회
     * - ID, Email, Password, Role, Authority
     */
    @Override public UUID getUserId() { return user.getId(); }
    @Override public String getUsername() { return user.getEmail(); } // Username => Email
    @Override public String getPassword() { return user.getPassword(); }
    @Override public UserRole getUserRole() { return user.getRole(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        user.getRole().toAuthority()
                )
        );
    }


    /**
     * 계정 상태 조회
     * - 계정 만료 여부, 계정 잠금 여부, 비밀번호 만료 여부, 계정 활성화 여부
     */
    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        // TODO: ex) 비밀번호 n회 잘못 입력 시 처리
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // TODO: ex) 90일 주기로 비밀번호 변경하는 정책 존재 시 처리
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        // TODO: ex) 관리자의 계정 정지, 이메일 미인증 시 처리
        return UserDetails.super.isEnabled();
    }
}
