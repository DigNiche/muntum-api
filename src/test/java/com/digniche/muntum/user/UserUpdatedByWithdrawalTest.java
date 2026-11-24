package com.digniche.muntum.user;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curationlike.service.CurationLikeService;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 탈퇴 시 다른 회원 행의 수정자(users.updated_by) 치환
 * - 관리자가 다른 회원 행을 바꾸면(지원서 승인의 역할 변경 등) updated_by에 관리자가 자동으로 들어간다
 * - 관리자 탈퇴 트랜잭션은 회원 행 잠금 바로 다음, 큐레이션을 잠그기 전에 이 값을 탈퇴자 UUID로 바꾼다
 * - idx_users_updated_by로 해당 회원 행만 잠가 다른 회원의 요청을 막지 않는다
 * - 잠금 대기 여부는 performance_schema.data_lock_waits로 확인한다 (그래서 root 계정으로 접속)
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CurationLikeService.class, UserUpdatedByWithdrawalTest.TestAuditConfig.class})
class UserUpdatedByWithdrawalTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.45")
            .withUsername("root")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci")
            .withEnv("TZ", "Asia/Seoul");

    // 감사 칸에 "지금 요청한 회원"이 들어가도록 테스트에서 바꿀 수 있게 둔다
    static final AtomicReference<UUID> CURRENT_AUDITOR = new AtomicReference<>(UUID.randomUUID());

    @TestConfiguration
    @EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
    static class TestAuditConfig {
        @Bean
        AuditorAware<UUID> testAuditorAware() {
            return () -> Optional.of(CURRENT_AUDITOR.get());
        }
    }

    @Autowired
    UserRepository userRepository;
    @Autowired
    CurationLikeService curationLikeService;
    @Autowired
    EntityManager em;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM curation_likes");
        jdbcTemplate.update("DELETE FROM curations");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM users");
    }


    // ===== 치환 결과 =====

    @Test
    void 관리자가_역할을_바꾼_회원_행은_관리자_탈퇴_후_수정자가_탈퇴_관리자_UUID가_된다() {
        UUID managerId = saveUser(UserRole.MANAGER);
        UUID otherManagerId = saveUser(UserRole.MANAGER);
        UUID promoted = saveUser(UserRole.AUDIENCE);
        UUID promotedByOther = saveUser(UserRole.AUDIENCE);
        // 지원서 승인과 같은 경로: 관리자가 요청한 트랜잭션에서 지원자 역할을 바꾼다 → 감사 기능이 updated_by에 관리자를 넣는다
        promoteAs(managerId, promoted);
        promoteAs(otherManagerId, promotedByOther);
        assertThat(updatedBy(promoted)).isEqualTo(managerId);
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", managerId);

        Integer replaced = transactionTemplate.execute(status -> {
            userRepository.findByIdForUpdate(managerId).orElseThrow();
            int count = userRepository.replaceUpdatedByWith(managerId, withdrawn);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(managerId));
            return count;
        });

        assertThat(replaced).isEqualTo(1);
        assertThat(updatedBy(promoted)).isEqualTo(withdrawn);
        assertThat(updatedBy(promotedByOther)).isEqualTo(otherManagerId);
    }

    @Test
    void 치환은_수정자_인덱스로_해당_행만_찾는다() {
        UUID managerId = saveUser(UserRole.MANAGER);

        Map<String, Object> plan = jdbcTemplate.queryForMap(
                "EXPLAIN UPDATE users SET updated_by = ? WHERE updated_by = ?",
                toBytes(UUID.randomUUID()), toBytes(managerId));

        assertThat(plan.get("key")).isEqualTo("idx_users_updated_by");
        assertThat(plan.get("type")).isNotEqualTo("ALL");
    }


    // ===== 잠금 =====

    @Test
    void 관리자_탈퇴가_치환한_뒤에도_관계없는_회원의_좋아요는_기다리지_않고_대상_회원만_잠깐_기다린다() throws Exception {
        UUID managerId = saveUser(UserRole.MANAGER);
        UUID promoted = saveUser(UserRole.AUDIENCE);
        UUID unrelated = saveUser(UserRole.AUDIENCE);
        promoteAs(managerId, promoted);
        UUID curationId = savePublishedCuration();
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", managerId);

        CountDownLatch replacedLatch = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> withdrawal = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            // UserService.completeWithdrawal과 같은 순서: 회원 행 잠금 → 다른 회원 행 수정자 치환 → (큐레이션 단계…)
            userRepository.findByIdForUpdate(managerId).orElseThrow();
            userRepository.replaceUpdatedByWith(managerId, withdrawn);
            replacedLatch.countDown();
            await(release);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(managerId));
        }));
        await(replacedLatch);

        // 관계없는 회원의 좋아요: 회원 행 공유 잠금 → 큐레이션 행. 기다리지 않고 바로 끝난다
        CompletableFuture.runAsync(() -> curationLikeService.like(curationId, unrelated, UserRole.AUDIENCE))
                .get(5, TimeUnit.SECONDS);
        assertThat(lockWaitCount()).isZero();

        // 치환 대상 회원의 좋아요: 탈퇴 트랜잭션이 그 회원 행을 쥐고 있어 끝날 때까지 기다린다(교착 아님)
        CompletableFuture<Void> promotedLike = CompletableFuture.runAsync(
                () -> curationLikeService.like(curationId, promoted, UserRole.CURATOR));
        awaitLockWaits(1);

        release.countDown();
        withdrawal.get(10, TimeUnit.SECONDS);
        promotedLike.get(10, TimeUnit.SECONDS);

        assertThat(updatedBy(promoted)).isEqualTo(withdrawn);
        assertThat(likeCount(curationId)).isEqualTo(2);
    }


    // ===== 도우미 =====

    private void promoteAs(UUID managerId, UUID userId) {
        CURRENT_AUDITOR.set(managerId);
        try {
            transactionTemplate.executeWithoutResult(status -> em.find(User.class, userId).promoteToCurator());
        } finally {
            CURRENT_AUDITOR.set(UUID.randomUUID());
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
                    .title("수정자 치환 테스트 프로그램")
                    .programType(ProgramType.EXHIBITION)
                    .description("수정자 치환 테스트 프로그램 소개")
                    .free(true)
                    .venueName("테스트 장소")
                    .address("서울시 테스트구")
                    .latitude(new BigDecimal("37.5"))
                    .longitude(new BigDecimal("127.0"))
                    .build();
            em.persist(program);

            Curation curation = Curation.builder()
                    .curatorId(curatorId)
                    .submittedProgramTitle("수정자 치환 테스트 프로그램")
                    .submittedPlace("테스트 장소")
                    .tagline("한 줄 소개")
                    .content("큐레이션 본문")
                    .build();
            em.persist(curation);
            curation.approve(program, UUID.randomUUID());
            return curation.getId();
        });
    }

    private UUID updatedBy(UUID userId) {
        byte[] value = jdbcTemplate.queryForObject("SELECT updated_by FROM users WHERE id = ?", byte[].class, toBytes(userId));
        return value == null ? null : toUuid(value);
    }

    private int likeCount(UUID curationId) {
        return jdbcTemplate.queryForObject("SELECT like_count FROM curations WHERE id = ?", Integer.class, toBytes(curationId));
    }

    private void awaitLockWaits(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (lockWaitCount() < expected) {
            assertThat(System.nanoTime()).as("잠금 대기 %d개를 기다리다 시간 초과", expected).isLessThan(deadline);
            Thread.sleep(20);
        }
    }

    private int lockWaitCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    private static UUID toUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
