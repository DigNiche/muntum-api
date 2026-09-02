package com.digniche.muntum;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR 시, 데이터 마이그레이션에 대해 테스트
 * - 빈 MySQL 컨테이너에 Flyway 마이그레이션 V1부터 적용 후
 * - validate 옵션을 통해 모든 Entity를 Schema와 대조
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class FlywayMigrationValidationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.45")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci")
            .withEnv("TZ", "Asia/Seoul");

    @Autowired
    Flyway flyway;

    @Test
    @DisplayName("Entity와 Schema 일치 - Based On 데이터 마이그레이션 V1 Baseline")
    void migrationsApplyCleanlyAndSchemaMatchesEntities() {
        var applied = flyway.info().applied();

        assertThat(applied).as("최소 V1은 적용되어야 한다").isNotEmpty();
        assertThat(applied)
                .as("적용된 마이그레이션 중 실패 상태가 없어야 한다")
                .allSatisfy(m -> assertThat(m.getState().isFailed()).isFalse());

        for (var m : flyway.info().all()) {
            System.out.printf("[flyway] %-16s %-12s %s%n",
                    m.getVersion(), m.getState(), m.getDescription());
        }
    }
}