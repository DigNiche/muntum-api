package com.digniche.muntum.program;

import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.entity.KeywordType;
import com.digniche.muntum.keyword.repository.ProgramKeywordRepository;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.entity.ProgramKeyword;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로그램·프로그램 이미지·프로그램 키워드의 사용자 칸 탈퇴 처리와 정리 마이그레이션
 * - 프로그램·이미지·키워드는 관리자(또는 시스템)만 다룬다 → 관리자 탈퇴 때만 탈퇴자 UUID로 바꾼다
 * - V202611231100: 레거시 큐레이터 칸을 지정 관리자로, 사라진 회원 값을 탈퇴 관리자 UUID로 바꾼다
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(ProgramWithdrawalTest.TestAuditConfig.class)
class ProgramWithdrawalTest {

    private static final String MIGRATION = "db/migration/V202611231100__replace_legacy_and_dangling_user_ids_on_programs.sql";
    private static final UUID LEGACY_CURATOR = UUID.fromString("014d9cea-fd6b-44a3-8d74-d96ac68c00d0");
    private static final UUID ASSIGNED_MANAGER = UUID.fromString("cf48de50-db8d-4d84-b524-3783f2c2d8b5");
    private static final List<String> USER_COLUMNS = List.of("created_by", "updated_by", "deleted_by");
    private static final List<String> TABLES = List.of("programs", "program_images", "program_keywords");

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
    ProgramRepository programRepository;
    @Autowired
    ProgramImageRepository programImageRepository;
    @Autowired
    ProgramKeywordRepository programKeywordRepository;
    @Autowired
    EntityManager em;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    DataSource dataSource;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM program_keywords");
        jdbcTemplate.update("DELETE FROM program_images");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM keywords");
        jdbcTemplate.update("DELETE FROM users");
    }


    // ===== 관리자 탈퇴 =====

    @Test
    void 관리자가_탈퇴하면_프로그램_이미지_키워드의_사용자_칸이_모두_탈퇴_관리자_UUID가_되고_다른_회원_칸은_그대로다() {
        UUID managerId = saveUser(UserRole.MANAGER);
        UUID otherManagerId = saveUser(UserRole.MANAGER);
        ProgramRows mine = saveProgramWithImageAndKeyword("탈퇴관리자키워드");
        ProgramRows others = saveProgramWithImageAndKeyword("다른관리자키워드");
        setAllUserColumns(mine, managerId);
        setAllUserColumns(others, otherManagerId);
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", managerId);

        // UserService.completeWithdrawal의 관리자 처리 중 프로그램·이미지·키워드 부분 + 회원 행 삭제
        transactionTemplate.executeWithoutResult(status -> {
            programRepository.replaceCreatedByWith(managerId, withdrawn);
            programRepository.replaceUpdatedByWith(managerId, withdrawn);
            programRepository.replaceDeletedByWith(managerId, withdrawn);
            programImageRepository.replaceCreatedByWith(managerId, withdrawn);
            programImageRepository.replaceUpdatedByWith(managerId, withdrawn);
            programImageRepository.replaceDeletedByWith(managerId, withdrawn);
            programKeywordRepository.replaceCreatedByWith(managerId, withdrawn);
            programKeywordRepository.replaceUpdatedByWith(managerId, withdrawn);
            programKeywordRepository.replaceDeletedByWith(managerId, withdrawn);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(managerId));
        });

        assertAllUserColumns(mine, withdrawn);
        assertAllUserColumns(others, otherManagerId);
    }


    // ===== 마이그레이션: 레거시 큐레이터 =====

    @Test
    void 마이그레이션은_레거시_큐레이터_칸만_지정_관리자로_바꾸고_큐레이터_회원은_그대로_둔다() {
        saveUserWithId(UserRole.CURATOR, LEGACY_CURATOR);
        saveUserWithId(UserRole.MANAGER, ASSIGNED_MANAGER);
        UUID otherManagerId = saveUser(UserRole.MANAGER);
        ProgramRows legacy = saveProgramWithImageAndKeyword("레거시키워드");
        ProgramRows others = saveProgramWithImageAndKeyword("관리자키워드");
        setAllUserColumns(legacy, LEGACY_CURATOR);
        setAllUserColumns(others, otherManagerId);

        runMigration();

        assertAllUserColumns(legacy, ASSIGNED_MANAGER);
        assertAllUserColumns(others, otherManagerId);
        assertThat(jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, toBytes(LEGACY_CURATOR)))
                .isEqualTo("CURATOR");
    }

    @Test
    void 지정_관리자가_관리자로_없으면_레거시_칸을_바꾸지_않는다() {
        saveUserWithId(UserRole.CURATOR, LEGACY_CURATOR);
        saveUserWithId(UserRole.CURATOR, ASSIGNED_MANAGER);   // 관리자가 아니게 된 경우
        ProgramRows legacy = saveProgramWithImageAndKeyword("레거시키워드2");
        setAllUserColumns(legacy, LEGACY_CURATOR);

        runMigration();

        assertAllUserColumns(legacy, LEGACY_CURATOR);
    }


    // ===== 마이그레이션: 사라진 회원 =====

    @Test
    void 마이그레이션은_사라진_회원_값을_탈퇴_관리자_UUID로_바꾸고_회원_탈퇴자_시스템_값은_건드리지_않는다() {
        UUID memberId = saveUser(UserRole.MANAGER);
        UUID missing = UUID.randomUUID();
        UUID withdrawnCurator = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", UUID.randomUUID());
        ProgramRows dangling = saveProgramWithImageAndKeyword("사라진회원키워드");
        ProgramRows member = saveProgramWithImageAndKeyword("회원키워드");
        ProgramRows special = saveProgramWithImageAndKeyword("특수키워드");
        setAllUserColumns(dangling, missing);
        setAllUserColumns(member, memberId);
        setAllUserColumns(special, AuditorAwareImpl.SYSTEM_UUID);
        setColumn("programs", special.programId(), "deleted_by", withdrawnCurator);

        runMigration();

        assertAllUserColumns(dangling, AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", missing));
        assertAllUserColumns(member, memberId);
        assertThat(column("programs", special.programId(), "created_by")).isEqualTo(AuditorAwareImpl.SYSTEM_UUID);
        assertThat(column("programs", special.programId(), "deleted_by")).isEqualTo(withdrawnCurator);
        assertThat(column("program_images", special.imageId(), "updated_by")).isEqualTo(AuditorAwareImpl.SYSTEM_UUID);
    }


    // ===== 도우미 =====

    private record ProgramRows(UUID programId, UUID imageId, UUID programKeywordId) {
        UUID idOf(String table) {
            return switch (table) {
                case "programs" -> programId;
                case "program_images" -> imageId;
                case "program_keywords" -> programKeywordId;
                default -> throw new IllegalArgumentException(table);
            };
        }
    }

    private ProgramRows saveProgramWithImageAndKeyword(String keywordName) {
        return transactionTemplate.execute(status -> {
            Program program = Program.builder()
                    .title("탈퇴 테스트 프로그램")
                    .programType(ProgramType.EXHIBITION)
                    .description("탈퇴 테스트 프로그램 소개")
                    .free(true)
                    .venueName("테스트 장소")
                    .address("서울시 테스트구")
                    .latitude(new BigDecimal("37.5"))
                    .longitude(new BigDecimal("127.0"))
                    .build();
            em.persist(program);
            ProgramImage image = ProgramImage.builder().program(program).imageUrl("https://storage/p.jpg").displayOrder(1).build();
            em.persist(image);
            Keyword keyword = Keyword.builder().name(keywordName).type(KeywordType.THEME).build();
            em.persist(keyword);
            ProgramKeyword programKeyword = ProgramKeyword.builder().program(program).keyword(keyword).build();
            em.persist(programKeyword);
            return new ProgramRows(program.getId(), image.getId(), programKeyword.getId());
        });
    }

    private void setAllUserColumns(ProgramRows rows, UUID userId) {
        for (String table : TABLES) {
            for (String column : USER_COLUMNS) {
                setColumn(table, rows.idOf(table), column, userId);
            }
        }
    }

    private void assertAllUserColumns(ProgramRows rows, UUID expected) {
        for (String table : TABLES) {
            for (String column : USER_COLUMNS) {
                assertThat(column(table, rows.idOf(table), column)).as(table + "." + column).isEqualTo(expected);
            }
        }
    }

    private void setColumn(String table, UUID rowId, String column, UUID value) {
        jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", toBytes(value), toBytes(rowId));
    }

    private UUID column(String table, UUID rowId, String column) {
        byte[] value = jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM " + table + " WHERE id = ?", byte[].class, toBytes(rowId));
        return value == null ? null : toUuid(value);
    }

    private void runMigration() {
        new ResourceDatabasePopulator(new ClassPathResource(MIGRATION)).execute(dataSource);
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

    // 운영의 특정 회원 id를 재현하려고 저장 뒤 id를 바꾼다 (users를 가리키는 행이 아직 없을 때만 호출)
    private void saveUserWithId(UserRole role, UUID id) {
        UUID generated = saveUser(role);
        jdbcTemplate.update("UPDATE users SET id = ? WHERE id = ?", toBytes(id), toBytes(generated));
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
