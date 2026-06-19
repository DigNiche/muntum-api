package com.digniche.muntum.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 엔티티 - 사용자 프로필 정보
 * - BaseEntity의 JpaAuditing 활성화
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(auth -> auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails)
                .map(auth -> ((CustomUserDetails) auth.getPrincipal()).getUserId());
    }
}
