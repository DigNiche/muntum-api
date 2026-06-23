package com.digniche.muntum.global.config;

import com.digniche.muntum.global.security.UserPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Auditor 조회 구현 컴포넌트
 * - 현재 로그인한 사용자 정보로 Auditing 필드 채움
 */
@Component
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return Optional.ofNullable(authentication)                                 // Null 여부 확인
                .filter(Authentication::isAuthenticated)                           // 인증 여부 확인
                .map(Authentication::getPrincipal)                                 // Get Principal
                .filter(principal -> principal instanceof UserPrincipal)    // DB와 일치하는 User 정보
                .map(principal -> ((UserPrincipal) principal).getUserId());
    }
}
