package com.digniche.muntum.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치 스케줄 설정
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
