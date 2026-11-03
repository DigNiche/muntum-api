package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.exception.GlobalExceptionHandler;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 락 대기 시간 초과(MySQL 1205)는 "잠시 후 다시 시도" 에러(G004, 503)로 응답한다
 * - 락 대기 시간을 1초로 줄인 MySQL 컨테이너를 따로 둔다 (다른 테스트의 동시 요청에 영향을 주지 않도록)
 * - 다른 트랜잭션이 큐레이션 행을 잡고 있는 동안 좋아요를 보내 실제로 1205를 일으킨다
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CurationLikeService.class, CurationLikeLockTimeoutTest.TestAuditConfig.class})
class CurationLikeLockTimeoutTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.45")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci",
                    "--innodb-lock-wait-timeout=1")
            .withEnv("TZ", "Asia/Seoul");

    // created_by NOT NULL을 채우기 위한 테스트용 감사(Audit) 설정 (@DataJpaTest는 운영용 AuditConfig를 불러오지 않음)
    @TestConfiguration
    @EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
    static class TestAuditConfig {
        @Bean
        AuditorAware<UUID> testAuditorAware() {
            UUID auditor = UUID.randomUUID();
            return () -> Optional.of(auditor);
        }
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Autowired
    CurationLikeService curationLikeService;

    @Autowired
    EntityManager em;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 커밋된 데이터가 남지 않도록 FK 역순으로 지운다
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM curation_likes");
        jdbcTemplate.update("DELETE FROM curations");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 큐레이션_행_락을_기다리다_시간이_초과되면_잠시_후_다시_시도_에러로_응답한다() throws Exception {
        UUID curationId = savePublishedCuration();
        UUID userId = saveUser(UserRole.AUDIENCE);

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> holder = CompletableFuture.runAsync(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    jdbcTemplate.queryForList("SELECT id FROM curations WHERE id = ? FOR UPDATE", toBytes(curationId));
                    locked.countDown();
                    await(release);
                }));
        await(locked);

        DataAccessException e;
        try {
            e = catchThrowableOfType(
                    () -> curationLikeService.like(curationId, userId, UserRole.AUDIENCE),
                    DataAccessException.class);
        } finally {
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
        }

        assertThat(e).as("락 대기 시간 초과로 실패해야 한다").isNotNull();
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleDataAccessException(e, new MockHttpServletRequest("PUT", "/api/v1/curations/" + curationId + "/like"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.RETRY_LATER.getCode());
        assertThat(likeRowCount(curationId)).isZero();
        assertThat(storedLikeCount(curationId)).isZero();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private UUID saveUser(UserRole role) {
        return transactionTemplate.execute(status -> {
            User user = User.builder()
                    .email(UUID.randomUUID() + "@test.com")
                    .role(role)
                    .build();
            em.persist(user);
            return user.getId();
        });
    }

    private UUID savePublishedCuration() {
        UUID curatorId = saveUser(UserRole.CURATOR);
        return transactionTemplate.execute(status -> {
            Program program = Program.builder()
                    .title("락 대기 테스트 프로그램")
                    .programType(ProgramType.EXHIBITION)
                    .description("락 대기 테스트 프로그램 소개")
                    .free(true)
                    .venueName("테스트 장소")
                    .address("서울시 테스트구")
                    .latitude(new BigDecimal("37.5"))
                    .longitude(new BigDecimal("127.0"))
                    .build();
            em.persist(program);

            Curation curation = Curation.builder()
                    .curatorId(curatorId)
                    .submittedProgramTitle("락 대기 테스트 프로그램")
                    .submittedPlace("테스트 장소")
                    .tagline("한 줄 소개")
                    .content("큐레이션 본문")
                    .build();
            em.persist(curation);
            curation.approve(program, UUID.randomUUID());
            return curation.getId();
        });
    }

    private int likeRowCount(UUID curationId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM curation_likes WHERE curation_id = ?",
                Integer.class, toBytes(curationId));
    }

    private int storedLikeCount(UUID curationId) {
        return jdbcTemplate.queryForObject(
                "SELECT like_count FROM curations WHERE id = ?",
                Integer.class, toBytes(curationId));
    }

    private static byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}
