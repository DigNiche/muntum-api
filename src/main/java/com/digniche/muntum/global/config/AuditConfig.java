package com.digniche.muntum.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Audit 설정
 * - BaseEntity의 JpaAuditing 활성화
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")  // AuditorAwareImpl
public class AuditConfig {
}
