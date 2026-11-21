package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.Curation;
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
import org.springframework.dao.DataIntegrityViolationException;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 회원 한 명의 큐레이션 좋아요 활동 정리(clearLikeActivity)와 탈퇴·좋아요 동시 요청
 * - 탈퇴 트랜잭션은 UserService.completeWithdrawal의 앞부분과 같은 순서로 흉내 낸다
 *   (회원 행 배타 잠금 → 좋아요 정리 → ... → 회원 행 삭제)
 * - 잠금 대기 여부는 performance_schema.data_lock_waits로 확인한다 (그래서 root 계정으로 접속)
 * - 테스트 트랜잭션을 끄고(NOT_SUPPORTED) 요청마다 실제로 커밋한다
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CurationLikeService.class, CurationLikeClearActivityTest.TestAuditConfig.class})
class CurationLikeClearActivityTest {

    private static final int RACE_ITERATIONS = 30;

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.45")
            .withUsername("root")
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
    UserRepository userRepository;

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


    // ===== 정리 결과 =====

    @Test
    void 그_회원의_좋아요만_지우고_그_큐레이션들의_좋아요_수만_1씩_줄인다() {
        UUID first = savePublishedCuration();
        UUID second = savePublishedCuration();
        UUID untouched = savePublishedCuration();
        UUID leaving = saveUser(UserRole.AUDIENCE);
        UUID staying = saveUser(UserRole.CURATOR);
        like(first, leaving);
        like(second, leaving);
        like(first, staying);
        like(untouched, staying);

        withdraw(leaving, () -> { });

        assertThat(likeRowCountOfUser(leaving)).isZero();
        assertThat(likeRowCount(first)).isEqualTo(1);
        assertThat(storedLikeCount(first)).isEqualTo(1);
        assertThat(likeRowCount(second)).isZero();
        assertThat(storedLikeCount(second)).isZero();
        assertThat(storedLikeCount(untouched)).isEqualTo(1);
    }

    @Test
    void 좋아요가_없는_회원이면_아무것도_바꾸지_않는다() {
        UUID curationId = savePublishedCuration();
        UUID staying = saveUser(UserRole.AUDIENCE);
        UUID leaving = saveUser(UserRole.AUDIENCE);
        like(curationId, staying);

        withdraw(leaving, () -> { });

        assertThat(likeRowCount(curationId)).isEqualTo(1);
        assertThat(storedLikeCount(curationId)).isEqualTo(1);
    }

    @Test
    void 삭제되거나_비공개가_된_큐레이션의_좋아요도_정리한다() {
        UUID deleted = savePublishedCuration();
        UUID unpublished = savePublishedCuration();
        UUID leaving = saveUser(UserRole.AUDIENCE);
        like(deleted, leaving);
        like(unpublished, leaving);
        jdbcTemplate.update("UPDATE curations SET deleted_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now()), toBytes(deleted));
        jdbcTemplate.update("UPDATE curations SET publication_status = 'UNPUBLISHED' WHERE id = ?", toBytes(unpublished));

        withdraw(leaving, () -> { });

        assertThat(likeRowCountOfUser(leaving)).isZero();
        assertThat(storedLikeCount(deleted)).isZero();
        assertThat(storedLikeCount(unpublished)).isZero();
    }

    @Test
    void 탈퇴가_끝난_회원의_좋아요는_USER_NOT_FOUND로_거절하고_아무것도_남기지_않는다() {
        UUID curationId = savePublishedCuration();
        UUID leaving = saveUser(UserRole.AUDIENCE);
        withdraw(leaving, () -> { });

        BusinessException e = catchThrowableOfType(
                BusinessException.class,
                () -> curationLikeService.like(curationId, leaving, UserRole.AUDIENCE));

        assertThat(e).isNotNull();
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(likeRowCount(curationId)).isZero();
        assertThat(storedLikeCount(curationId)).isZero();
    }


    // ===== 잠금 순서 =====

    @Test
    void 탈퇴_중인_회원의_좋아요는_회원_행에서_기다리고_같은_큐레이션의_다른_회원은_막지_않는다() throws Exception {
        UUID curationId = savePublishedCuration();
        UUID leaving = saveUser(UserRole.AUDIENCE);
        UUID other = saveUser(UserRole.AUDIENCE);

        CountDownLatch userLocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> withdrawal = CompletableFuture.runAsync(() -> withdraw(leaving, () -> {
            userLocked.countDown();
            await(release);
        }));
        await(userLocked);

        // 탈퇴 중인 회원의 좋아요: 회원 행 공유 잠금에서 기다린다 (큐레이션 행은 아직 잡지 않음)
        CompletableFuture<Void> leavingLike = CompletableFuture.runAsync(
                () -> curationLikeService.like(curationId, leaving, UserRole.AUDIENCE));
        awaitLockWaits(1);

        // 같은 큐레이션에 다른 회원의 좋아요: 기다리지 않고 바로 끝난다
        CompletableFuture.runAsync(() -> curationLikeService.like(curationId, other, UserRole.AUDIENCE))
                .get(5, TimeUnit.SECONDS);
        assertThat(lockWaitCount()).isEqualTo(1);

        release.countDown();
        withdrawal.get(10, TimeUnit.SECONDS);

        // 탈퇴가 끝난 뒤 깨어난 좋아요는 FK 오류(500)가 아니라 USER_NOT_FOUND
        assertThat(causeOf(leavingLike)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        assertThat(likeRowCount(curationId)).isEqualTo(1);
        assertThat(storedLikeCount(curationId)).isEqualTo(1);
    }

    /**
     * 회원 행 공유 잠금 없이 예전 순서(큐레이션 행 → 좋아요 INSERT의 FK 검사)로 좋아요를 넣으면 생기는 문제
     * - 좋아요 서비스가 아니라 같은 SQL을 직접 실행해 예전 동작을 재현한다
     */
    @Test
    void 회원_행을_먼저_잠그지_않으면_다른_회원이_막히고_탈퇴_뒤_FK_오류가_난다() throws Exception {
        UUID curationId = savePublishedCuration();
        UUID leaving = saveUser(UserRole.AUDIENCE);
        UUID other = saveUser(UserRole.AUDIENCE);

        CountDownLatch userLocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> withdrawal = CompletableFuture.runAsync(() -> withdraw(leaving, () -> {
            userLocked.countDown();
            await(release);
        }));
        await(userLocked);

        // 예전 순서의 좋아요: 큐레이션 행을 잡은 채 FK 검사에서 회원 행을 기다린다
        CompletableFuture<Void> oldOrderLike = CompletableFuture.runAsync(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    jdbcTemplate.queryForList("SELECT id FROM curations WHERE id = ? FOR UPDATE", toBytes(curationId));
                    jdbcTemplate.update("INSERT INTO curation_likes (user_id, curation_id, created_at) VALUES (?, ?, ?)",
                            toBytes(leaving), toBytes(curationId), Timestamp.valueOf(LocalDateTime.now()));
                }));
        awaitLockWaits(1);

        // 다른 회원의 좋아요가 그 큐레이션 행에서 탈퇴가 끝날 때까지 막힌다
        CompletableFuture<Void> otherLike = CompletableFuture.runAsync(
                () -> curationLikeService.like(curationId, other, UserRole.AUDIENCE));
        awaitLockWaits(2);
        assertThat(otherLike).isNotDone();

        release.countDown();
        withdrawal.get(10, TimeUnit.SECONDS);

        // 깨어난 예전 순서 좋아요는 FK 오류(1452)로 실패한다 → 서비스였다면 500
        assertThat(causeOf(oldOrderLike)).isInstanceOf(DataIntegrityViolationException.class);
        otherLike.get(10, TimeUnit.SECONDS);
        assertThat(likeRowCount(curationId)).isEqualTo(1);
    }


    // ===== 동시 요청 반복 =====

    /**
     * 탈퇴 + 같은 회원의 취소·좋아요 + 다른 회원의 좋아요·취소를 한꺼번에 여러 번 보낸다
     * - 데드락(1213)이나 FK 오류 없이, 실패는 탈퇴 뒤 좋아요의 USER_NOT_FOUND뿐이어야 한다
     * - 끝난 뒤 좋아요 수와 실제 좋아요 행 수가 같아야 한다
     */
    @Test
    void 탈퇴와_좋아요_취소가_동시에_와도_데드락_없이_좋아요_수가_맞다() throws Exception {
        UUID curationId = savePublishedCuration();
        UUID other = saveUser(UserRole.AUDIENCE);

        List<Throwable> unexpected = new ArrayList<>();
        for (int i = 0; i < RACE_ITERATIONS; i++) {
            UUID leaving = saveUser(UserRole.AUDIENCE);
            like(curationId, leaving);

            List<Throwable> failures = runConcurrently(List.of(
                    () -> withdraw(leaving, () -> { }),
                    () -> {
                        curationLikeService.cancelLike(curationId, leaving, UserRole.AUDIENCE);
                        curationLikeService.like(curationId, leaving, UserRole.AUDIENCE);
                    },
                    () -> {
                        curationLikeService.like(curationId, other, UserRole.AUDIENCE);
                        curationLikeService.cancelLike(curationId, other, UserRole.AUDIENCE);
                    }
            ));
            failures.stream()
                    .filter(f -> !(f instanceof BusinessException be && be.getErrorCode() == ErrorCode.USER_NOT_FOUND))
                    .forEach(unexpected::add);

            assertThat(likeRowCountOfUser(leaving)).as("%d번째: 탈퇴한 회원의 좋아요가 남으면 안 된다", i).isZero();
            assertThat(storedLikeCount(curationId)).as("%d번째: 좋아요 수", i).isEqualTo(likeRowCount(curationId));
        }

        assertThat(unexpected).isEmpty();
    }


    // ===== 실행 계획 =====

    @Test
    void 회원_행_공유_잠금_조회는_기본키로_한_행만_찾는다() {
        UUID userId = saveUser(UserRole.AUDIENCE);

        Map<String, Object> plan = jdbcTemplate.queryForMap(
                "EXPLAIN SELECT COUNT(*) FROM users WHERE id = ? FOR SHARE", toBytes(userId));

        assertThat(plan.get("type")).isEqualTo("const");
        assertThat(plan.get("key")).isEqualTo("PRIMARY");
    }

    /**
     * 큐레이션 여러 개 잠금 조회(CurationRepository.findAllByIdInForUpdate)가 기본키를 id 순서로 읽는지
     * - 행이 적으면 옵티마이저가 전체 스캔을 고를 수 있어, 큐레이션을 충분히 넣고 통계를 갱신한 뒤 확인한다
     * - 기본키 범위(range)로 읽고 filesort가 없어야 잠금 획득 순서가 id 순서라고 볼 수 있다
     */
    @Test
    void 큐레이션_여러_개_잠금_조회는_기본키를_id_순서로_읽는다() {
        UUID curatorId = saveUser(UserRole.CURATOR);
        List<UUID> curationIds = transactionTemplate.execute(status -> {
            List<UUID> ids = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                Curation curation = Curation.builder()
                        .curatorId(curatorId)
                        .submittedProgramTitle("실행 계획 확인용 " + i)
                        .submittedPlace("테스트 장소")
                        .tagline("한 줄 소개")
                        .content("큐레이션 본문")
                        .build();
                em.persist(curation);
                ids.add(curation.getId());
            }
            return ids;
        });
        jdbcTemplate.execute("ANALYZE TABLE curations");

        List<UUID> targets = curationIds.subList(0, 5);
        String placeholders = String.join(",", Collections.nCopies(targets.size(), "?"));
        Object[] params = targets.stream().map(CurationLikeClearActivityTest::toBytes).toArray();

        // Hibernate가 만드는 쿼리와 같은 모양: WHERE id IN (...) ORDER BY id FOR UPDATE
        Map<String, Object> plan = jdbcTemplate.queryForMap(
                "EXPLAIN SELECT * FROM curations c WHERE c.id IN (" + placeholders + ") ORDER BY c.id FOR UPDATE", params);

        assertThat(plan.get("type")).isEqualTo("range");
        assertThat(plan.get("key")).isEqualTo("PRIMARY");
        assertThat(String.valueOf(plan.get("Extra"))).doesNotContain("filesort");
    }


    // ===== 도우미 =====

    // UserService.completeWithdrawal의 잠금 순서를 흉내 낸 탈퇴 트랜잭션
    private void withdraw(UUID userId, Runnable whileHoldingUserLock) {
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            curationLikeService.clearLikeActivity(userId);
            whileHoldingUserLock.run();
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(userId));
        });
    }

    private void like(UUID curationId, UUID userId) {
        curationLikeService.like(curationId, userId, UserRole.AUDIENCE);
    }

    // 다른 트랜잭션의 잠금을 기다리는 요청이 expected개가 될 때까지 기다린다
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

    private static Throwable causeOf(CompletableFuture<?> future) throws InterruptedException {
        try {
            future.get(10, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError("작업이 끝나지 않았다", e);
        }
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
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        return failures;
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
                    .title("좋아요 정리 테스트 프로그램")
                    .programType(ProgramType.EXHIBITION)
                    .description("좋아요 정리 테스트 프로그램 소개")
                    .free(true)
                    .venueName("테스트 장소")
                    .address("서울시 테스트구")
                    .latitude(new BigDecimal("37.5"))
                    .longitude(new BigDecimal("127.0"))
                    .build();
            em.persist(program);

            Curation curation = Curation.builder()
                    .curatorId(curatorId)
                    .submittedProgramTitle("좋아요 정리 테스트 프로그램")
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

    private int likeRowCountOfUser(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM curation_likes WHERE user_id = ?",
                Integer.class, toBytes(userId));
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
