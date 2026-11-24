package com.digniche.muntum.keyword;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.dto.SelectKeywordsRequest;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.entity.KeywordType;
import com.digniche.muntum.keyword.entity.UserKeyword;
import com.digniche.muntum.keyword.service.TasteService;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 같은 회원의 관심 키워드 변경과 탈퇴가 동시에 일어날 때
 * - 둘 다 맨 앞에서 회원 행을 배타 잠금하므로, 나중에 온 쪽은 회원 행에서 기다렸다가 이어서 끝난다(교착 1213 없음)
 * - 관심 키워드 변경: TasteService.setTasteKeywords (회원 행 배타 잠금 → 관심 키워드 삭제·INSERT → tasteSelected 수정)
 * - 탈퇴: UserService.completeWithdrawal의 잠금 관련 부분만 같은 순서로 재현 (회원 행 배타 잠금 → 회원 행 DELETE, 관심 키워드는 FK CASCADE)
 * - 잠금 대기 여부는 performance_schema.data_lock_waits로 확인한다 (그래서 root 계정으로 접속)
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({TasteService.class, TasteKeywordWithdrawalConcurrencyTest.TestAuditConfig.class})
class TasteKeywordWithdrawalConcurrencyTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.45")
            .withUsername("root")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci")
            .withEnv("TZ", "Asia/Seoul");

    @TestConfiguration
    @EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
    static class TestAuditConfig {
        @Bean
        AuditorAware<UUID> testAuditorAware() {
            return () -> Optional.of(UUID.randomUUID());
        }
    }

    @Autowired
    TasteService tasteService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    EntityManager em;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM user_keywords");
        jdbcTemplate.update("DELETE FROM keywords");
        jdbcTemplate.update("DELETE FROM users");
    }


    @Test
    void 탈퇴가_회원_행을_먼저_잡으면_관심_키워드_변경은_기다렸다가_회원_없음으로_끝난다() throws Exception {
        List<String> before = saveKeywords(3);
        List<String> after = saveKeywords(3);
        UUID userId = saveUserWithKeywords(before);

        CountDownLatch lockedLatch = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> withdrawal = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            // UserService.completeWithdrawal과 같은 순서: 회원 행 배타 잠금 → (치환·정리 단계…) → 회원 행 삭제
            userRepository.findByIdForUpdate(userId).orElseThrow();
            lockedLatch.countDown();
            await(release);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(userId));
        }));
        await(lockedLatch);

        // 관심 키워드 변경: 맨 앞 회원 행 잠금에서 탈퇴가 끝나기를 기다린다
        CompletableFuture<Void> tasteChange = CompletableFuture.runAsync(
                () -> tasteService.setTasteKeywords(userId, new SelectKeywordsRequest(after)));
        awaitLockWaits(1);

        release.countDown();
        withdrawal.get(10, TimeUnit.SECONDS);

        // 탈퇴가 끝난 뒤 회원 행을 다시 읽으면 회원이 없다 → 교착·FK 오류가 아니라 회원 없음으로 끝난다
        assertThatThrownBy(() -> tasteChange.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        assertThat(userExists(userId)).isFalse();
        assertThat(userKeywordCount(userId)).isZero();
    }

    @Test
    void 관심_키워드_변경이_회원_행을_먼저_잡으면_탈퇴는_기다렸다가_새_관심_키워드까지_지운다() throws Exception {
        List<String> before = saveKeywords(3);
        List<String> after = saveKeywords(3);
        UUID userId = saveUserWithKeywords(before);

        CountDownLatch changedLatch = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        // 바깥 트랜잭션으로 감싸 관심 키워드 변경이 커밋 직전에 멈추게 한다(같은 트랜잭션에 참여)
        CompletableFuture<Void> tasteChange = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            tasteService.setTasteKeywords(userId, new SelectKeywordsRequest(after));
            em.flush();
            changedLatch.countDown();
            await(release);
        }));
        await(changedLatch);

        // 탈퇴: 맨 앞 회원 행 잠금에서 관심 키워드 변경이 끝나기를 기다린다
        CompletableFuture<Void> withdrawal = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            userRepository.findByIdForUpdate(userId).orElseThrow();
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(userId));
        }));
        awaitLockWaits(1);
        assertThat(userKeywordCount(userId)).as("변경이 커밋되기 전에는 이전 관심 키워드가 보인다").isEqualTo(3);

        release.countDown();
        tasteChange.get(10, TimeUnit.SECONDS);
        withdrawal.get(10, TimeUnit.SECONDS);

        // 탈퇴가 변경 커밋 뒤에 회원 행을 지워, 새로 넣은 관심 키워드도 CASCADE로 함께 지워졌다
        assertThat(userExists(userId)).isFalse();
        assertThat(userKeywordCount(userId)).isZero();
    }


    // ===== 도우미 =====

    private List<String> saveKeywords(int count) {
        return transactionTemplate.execute(status -> {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String name = "키워드-" + UUID.randomUUID().toString().substring(0, 8);
                em.persist(Keyword.builder().name(name).type(KeywordType.THEME).build());
                names.add(name);
            }
            return names;
        });
    }

    private UUID saveUserWithKeywords(List<String> keywordNames) {
        return transactionTemplate.execute(status -> {
            User user = User.builder()
                    .email(UUID.randomUUID() + "@test.com")
                    .role(UserRole.AUDIENCE)
                    .build();
            em.persist(user);
            em.createQuery("SELECT k FROM Keyword k WHERE k.name IN :names", Keyword.class)
                    .setParameter("names", keywordNames)
                    .getResultList()
                    .forEach(k -> em.persist(UserKeyword.builder().user(user).keyword(k).build()));
            return user.getId();
        });
    }

    private boolean userExists(UUID userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, toBytes(userId)) > 0;
    }

    private int userKeywordCount(UUID userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_keywords WHERE user_id = ?", Integer.class, toBytes(userId));
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
}
