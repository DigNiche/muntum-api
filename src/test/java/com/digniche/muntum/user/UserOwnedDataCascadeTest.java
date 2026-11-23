package com.digniche.muntum.user;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 회원 삭제 시 스크랩·관심 키워드·프로그램 반응을 DB가 함께 지우는지(FK ON DELETE CASCADE)
 * - 실제 MySQL에 Flyway 마이그레이션을 적용한 스키마로 확인
 * - 테스트 트랜잭션을 끄고(NOT_SUPPORTED) 실제로 커밋해, 다른 트랜잭션에서 결과를 보게 한다
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(UserOwnedDataCascadeTest.TestAuditConfig.class)
class UserOwnedDataCascadeTest {

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
    EntityManager em;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 커밋된 데이터가 다음 테스트에 남지 않도록 FK 역순으로 지운다
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM scraps");
        jdbcTemplate.update("DELETE FROM user_keywords");
        jdbcTemplate.update("DELETE FROM program_reactions");
        jdbcTemplate.update("DELETE FROM keywords");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 스크랩_관심키워드_프로그램반응의_회원_FK만_CASCADE이고_나머지는_그대로다() {
        Map<String, String> deleteRules = jdbcTemplate.query("""
                        SELECT k.TABLE_NAME, k.COLUMN_NAME, r.DELETE_RULE
                        FROM information_schema.KEY_COLUMN_USAGE k
                        JOIN information_schema.REFERENTIAL_CONSTRAINTS r
                          ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
                         AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
                        WHERE k.REFERENCED_TABLE_SCHEMA = DATABASE()
                          AND k.REFERENCED_TABLE_NAME = 'users'
                        """,
                (rs, i) -> Map.entry(rs.getString(1) + "." + rs.getString(2), rs.getString(3)))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(deleteRules).containsOnly(
                Map.entry("scraps.user_id", "CASCADE"),
                Map.entry("user_keywords.user_id", "CASCADE"),
                Map.entry("program_reactions.user_id", "CASCADE"),
                Map.entry("social_accounts.user_id", "NO ACTION"),
                Map.entry("user_terms_agreements.user_id", "NO ACTION"),
                Map.entry("curation_likes.user_id", "NO ACTION")
                // spot_suggestions.reviewed_by·informer는 FK 없는 UUID 칸 (V202611181400, V202611181500)
                // curator_applications.applicant_id도 FK 없는 UUID 칸 (V202611231000)
        );
    }

    @Test
    void 회원을_지우면_그_회원의_스크랩_관심키워드_프로그램반응만_함께_지워진다() {
        UUID programId = saveProgram();
        UUID keywordId = saveKeyword();
        UUID withdrawingUserId = saveUser();
        UUID otherUserId = saveUser();
        saveOwnedData(withdrawingUserId, programId, keywordId);
        saveOwnedData(otherUserId, programId, keywordId);

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(withdrawingUserId));

        assertThat(ownedRowCounts(withdrawingUserId)).containsExactly(0, 0, 0);
        assertThat(ownedRowCounts(otherUserId)).containsExactly(1, 1, 1);
    }

    @Test
    void 회원_삭제가_커밋되기_전에_들어온_스크랩은_삭제를_막지_못하고_스크랩_요청만_실패한다() throws Exception {
        UUID programId = saveProgram();
        UUID userId = saveUser();

        CountDownLatch userDeleted = new CountDownLatch(1);
        CountDownLatch scrapInsertStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // 회원 삭제 트랜잭션: 삭제한 뒤 스크랩 INSERT가 시작될 때까지 커밋을 미룬다
            CompletableFuture<Void> deleteUser = CompletableFuture.runAsync(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(userId));
                        userDeleted.countDown();
                        await(scrapInsertStarted);
                        sleepQuietly(300); // 스크랩 INSERT가 users 행 잠금에서 기다리는 상태가 되도록
                    }), executor);

            // 스크랩 트랜잭션: 회원 삭제가 커밋되기 전에 같은 회원의 스크랩을 넣는다
            CompletableFuture<Void> insertScrap = CompletableFuture.runAsync(() -> {
                await(userDeleted);
                scrapInsertStarted.countDown();
                transactionTemplate.executeWithoutResult(status ->
                        insertScrapRow(userId, programId));
            }, executor);

            deleteUser.get(30, TimeUnit.SECONDS);
            assertThatThrownBy(() -> insertScrap.get(30, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
        } finally {
            executor.shutdownNow();
        }

        assertThat(userCount(userId)).isZero();
        assertThat(ownedRowCounts(userId)).containsExactly(0, 0, 0);
    }

    private UUID saveUser() {
        return transactionTemplate.execute(status -> {
            User user = User.builder()
                    .email(UUID.randomUUID() + "@test.com")
                    .role(UserRole.AUDIENCE)
                    .build();
            em.persist(user);
            return user.getId();
        });
    }

    private UUID saveProgram() {
        return transactionTemplate.execute(status -> {
            Program program = Program.builder()
                    .title("CASCADE 테스트 프로그램")
                    .programType(ProgramType.EXHIBITION)
                    .description("CASCADE 테스트 프로그램 소개")
                    .free(true)
                    .venueName("테스트 장소")
                    .address("서울시 테스트구")
                    .latitude(new BigDecimal("37.5"))
                    .longitude(new BigDecimal("127.0"))
                    .build();
            em.persist(program);
            return program.getId();
        });
    }

    private UUID saveKeyword() {
        UUID keywordId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO keywords (id, created_at, created_by, is_active, name)
                        VALUES (?, ?, ?, b'1', ?)
                        """,
                toBytes(keywordId), now(), toBytes(UUID.randomUUID()), "키워드-" + keywordId);
        return keywordId;
    }

    private void saveOwnedData(UUID userId, UUID programId, UUID keywordId) {
        insertScrapRow(userId, programId);
        jdbcTemplate.update("""
                        INSERT INTO user_keywords (id, created_at, created_by, keyword_id, user_id)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                toBytes(UUID.randomUUID()), now(), toBytes(userId), toBytes(keywordId), toBytes(userId));
        jdbcTemplate.update("""
                        INSERT INTO program_reactions (id, user_id, program_id, reaction_type, created_at, created_by)
                        VALUES (?, ?, ?, 'LIKE', ?, ?)
                        """,
                toBytes(UUID.randomUUID()), toBytes(userId), toBytes(programId), now(), toBytes(userId));
    }

    private void insertScrapRow(UUID userId, UUID programId) {
        jdbcTemplate.update("""
                        INSERT INTO scraps (id, created_at, created_by, program_id, user_id)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                toBytes(UUID.randomUUID()), now(), toBytes(userId), toBytes(programId), toBytes(userId));
    }

    // [스크랩 수, 관심 키워드 수, 프로그램 반응 수]
    private int[] ownedRowCounts(UUID userId) {
        return new int[]{
                countByUser("scraps", userId),
                countByUser("user_keywords", userId),
                countByUser("program_reactions", userId)
        };
    }

    private int countByUser(String table, UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE user_id = ?", Integer.class, toBytes(userId));
    }

    private int userCount(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, toBytes(userId));
    }

    private static Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("대기 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}
