package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.Curation;
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
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 큐레이션 좋아요 동시 요청
 * - 테스트 트랜잭션을 끄고(NOT_SUPPORTED) 요청마다 실제로 커밋해, 다른 스레드가 서로의 데이터를 보게 한다
 * - 모든 요청이 끝난 뒤 like_count와 실제 좋아요 행 수가 같은지 확인
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CurationLikeService.class, CurationLikeConcurrencyTest.TestAuditConfig.class})
class CurationLikeConcurrencyTest {

    private static final int THREADS = 20;

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.45")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci")
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

    @Autowired
    CurationLikeService curationLikeService;

    @Autowired
    EntityManager em;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 커밋된 데이터가 다음 테스트에 남지 않도록 FK 역순으로 지운다
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM curation_likes");
        jdbcTemplate.update("DELETE FROM curations");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 여러_사용자가_동시에_좋아요를_눌러도_수와_좋아요_행_수가_같다() throws InterruptedException {
        UUID curationId = savePublishedCuration();
        List<UUID> userIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            userIds.add(saveUser(UserRole.AUDIENCE));
        }

        List<Throwable> failures = runConcurrently(userIds.stream()
                .<Runnable>map(userId -> () -> curationLikeService.like(curationId, userId, UserRole.AUDIENCE))
                .toList());

        assertThat(failures).isEmpty();
        assertThat(likeRowCount(curationId)).isEqualTo(THREADS);
        assertThat(storedLikeCount(curationId)).isEqualTo(THREADS);
    }

    @Test
    void 같은_사용자가_동시에_여러_번_좋아요를_눌러도_좋아요는_하나이고_수는_1이다() throws InterruptedException {
        UUID curationId = savePublishedCuration();
        UUID userId = saveUser(UserRole.AUDIENCE);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> curationLikeService.like(curationId, userId, UserRole.AUDIENCE));
        }
        List<Throwable> failures = runConcurrently(tasks);

        assertThat(failures).isEmpty();
        assertThat(likeRowCount(curationId)).isEqualTo(1);
        assertThat(storedLikeCount(curationId)).isEqualTo(1);
    }

    // 모든 작업을 같은 순간에 출발시키고, 끝날 때까지 기다린 뒤 실패한 예외 목록을 돌려준다
    private List<Throwable> runConcurrently(List<Runnable> tasks) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks.size());
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        for (Runnable task : tasks) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run();
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).as("모든 요청이 제한 시간 안에 끝나야 한다").isTrue();
        executor.shutdown();
        return failures;
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
                    .title("동시성 테스트 프로그램")
                    .programType(ProgramType.EXHIBITION)
                    .description("동시성 테스트 프로그램 소개")
                    .free(true)
                    .venueName("테스트 장소")
                    .address("서울시 테스트구")
                    .latitude(new BigDecimal("37.5"))
                    .longitude(new BigDecimal("127.0"))
                    .build();
            em.persist(program);

            Curation curation = Curation.builder()
                    .curatorId(curatorId)
                    .submittedProgramTitle("동시성 테스트 프로그램")
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
