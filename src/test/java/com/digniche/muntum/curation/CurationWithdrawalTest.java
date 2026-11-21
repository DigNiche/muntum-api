package com.digniche.muntum.curation;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationImage;
import com.digniche.muntum.curation.repository.CurationImageRepository;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.curation.service.CurationImageService;
import com.digniche.muntum.curation.service.CurationService;
import com.digniche.muntum.curationlike.service.CurationLikeQueryService;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.global.storage.ImageStorageService;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.service.ProgramService;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.service.CuratorProfileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 큐레이터 탈퇴 시 큐레이션 정리
 * - 프로그램과 연결되지 않은(program_id IS NULL) 큐레이션: 이미지와 함께 삭제, 스토리지 파일은 커밋 후 삭제
 * - 남는 큐레이션·이미지: 사용자 칸을 탈퇴자 UUID로 치환
 * - V202611201100 마이그레이션: 이미 남아 있던 사라진 회원 값을 탈퇴자 UUID로 치환
 * - 테스트 트랜잭션을 끄고(NOT_SUPPORTED) 실제로 커밋한다 (커밋 후 동작 확인)
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CurationService.class, CurationImageService.class, UserDisplayResolver.class, CurationWithdrawalTest.TestAuditConfig.class})
class CurationWithdrawalTest {

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

    // 큐레이션 정리와 관계없는 의존성
    @MockitoBean
    CuratorProfileService curatorProfileService;
    @MockitoBean
    CurationLikeQueryService curationLikeQueryService;
    @MockitoBean
    ProgramService programService;
    @MockitoBean
    ImageStorageService imageStorageService;

    @Autowired
    CurationService curationService;
    @Autowired
    CurationRepository curationRepository;
    @Autowired
    CurationImageRepository curationImageRepository;
    @Autowired
    EntityManager em;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    DataSource dataSource;

    @BeforeEach
    void resetStorageMock() {
        clearInvocations(imageStorageService);
    }

    // 커밋된 데이터가 다음 테스트에 남지 않도록 FK 역순으로 지운다
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM curation_images");
        jdbcTemplate.update("DELETE FROM curations");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM users");
    }


    // ===== 프로그램과 연결되지 않은 큐레이션 삭제 =====

    @Test
    void 프로그램과_연결되지_않은_큐레이션은_삭제된_것까지_이미지와_함께_지우고_파일은_커밋_후_지운다() {
        UUID curatorId = saveUser(UserRole.CURATOR);
        UUID pending = saveCuration(curatorId);
        UUID softDeleted = saveCuration(curatorId);
        softDelete(softDeleted, curatorId);
        saveImage(pending, "https://storage/pending-1.jpg", 1);
        saveImage(pending, "https://storage/pending-2.jpg", 2);
        saveImage(softDeleted, "https://storage/deleted-1.jpg", 1);

        transactionTemplate.executeWithoutResult(status -> {
            curationService.deleteNeverApprovedCurations(curatorId);
            // 커밋 전에는 파일을 지우지 않는다
            verify(imageStorageService, never()).delete(anyString());
        });

        assertThat(curationExists(pending)).isFalse();
        assertThat(curationExists(softDeleted)).isFalse();
        assertThat(imageCount(pending)).isZero();
        assertThat(imageCount(softDeleted)).isZero();
        verify(imageStorageService).delete("https://storage/pending-1.jpg");
        verify(imageStorageService).delete("https://storage/pending-2.jpg");
        verify(imageStorageService).delete("https://storage/deleted-1.jpg");
    }

    @Test
    void 프로그램과_연결된_큐레이션과_다른_큐레이터의_큐레이션은_남긴다() {
        UUID curatorId = saveUser(UserRole.CURATOR);
        UUID otherCuratorId = saveUser(UserRole.CURATOR);
        UUID approved = saveApprovedCuration(curatorId);
        UUID approvedThenResubmitted = saveApprovedCuration(curatorId);
        // 승인 뒤 수정 요청 → 재제출하면 status는 다시 PENDING이지만 프로그램 연결은 남는다
        jdbcTemplate.update("UPDATE curations SET status = 'PENDING' WHERE id = ?", toBytes(approvedThenResubmitted));
        UUID othersPending = saveCuration(otherCuratorId);
        saveImage(approved, "https://storage/approved-1.jpg", 1);

        transactionTemplate.executeWithoutResult(status -> curationService.deleteNeverApprovedCurations(curatorId));

        assertThat(curationExists(approved)).isTrue();
        assertThat(curationExists(approvedThenResubmitted)).isTrue();
        assertThat(curationExists(othersPending)).isTrue();
        assertThat(imageCount(approved)).isEqualTo(1);
        verify(imageStorageService, never()).delete(anyString());
    }

    @Test
    void 트랜잭션이_롤백되면_큐레이션_이미지_파일_모두_그대로_남는다() {
        UUID curatorId = saveUser(UserRole.CURATOR);
        UUID pending = saveCuration(curatorId);
        saveImage(pending, "https://storage/pending-1.jpg", 1);

        transactionTemplate.executeWithoutResult(status -> {
            curationService.deleteNeverApprovedCurations(curatorId);
            status.setRollbackOnly();
        });

        assertThat(curationExists(pending)).isTrue();
        assertThat(imageCount(pending)).isEqualTo(1);
        verify(imageStorageService, never()).delete(anyString());
    }


    // ===== 남는 큐레이션의 사용자 칸 치환 =====

    @Test
    void 남는_큐레이션과_이미지의_사용자_칸은_모두_탈퇴자_UUID로_바뀐다() {
        UUID curatorId = saveUser(UserRole.CURATOR);
        UUID approved = saveApprovedCuration(curatorId);
        saveImage(approved, "https://storage/approved-1.jpg", 1);
        setCurationUserColumns(approved, curatorId);
        setImageUserColumns(approved, curatorId);
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", curatorId);

        transactionTemplate.executeWithoutResult(status -> {
            curationRepository.replaceCuratorIdWith(curatorId, withdrawn);
            curationRepository.replaceCreatedByWith(curatorId, withdrawn);
            curationRepository.replaceUpdatedByWith(curatorId, withdrawn);
            curationRepository.replaceReviewedByWith(curatorId, withdrawn);
            curationRepository.replaceDeletedByWith(curatorId, withdrawn);
            curationImageRepository.replaceCreatedByWith(curatorId, withdrawn);
            curationImageRepository.replaceUpdatedByWith(curatorId, withdrawn);
        });

        // curator_id·created_by는 엔티티에서 updatable = false지만 JPQL 일괄 UPDATE로는 바뀐다
        for (String column : new String[]{"curator_id", "created_by", "updated_by", "reviewed_by", "deleted_by"}) {
            assertThat(curationColumn(approved, column)).as("curations.%s", column).isEqualTo(withdrawn);
        }
        for (String column : new String[]{"created_by", "updated_by"}) {
            assertThat(imageColumn(approved, column)).as("curation_images.%s", column).isEqualTo(withdrawn);
        }
    }


    // ===== V202611201100: 이미 남아 있던 사라진 회원 값 =====

    @Test
    void 마이그레이션은_사라진_작성자는_큐레이터로_사라진_검토자는_관리자_UUID로_바꾸고_회원_값은_건드리지_않는다() {
        UUID liveCurator = saveUser(UserRole.CURATOR);
        UUID liveManager = saveUser(UserRole.MANAGER);
        UUID goneCurator = UUID.randomUUID();   // users에 없는 큐레이터
        UUID goneManager = UUID.randomUUID();   // users에 없는 검토자

        // 운영의 그 1건과 같은 모양: 작성자 칸 4개 = 사라진 큐레이터, 검토자 = 사라진 관리자
        UUID broken = saveApprovedCuration(liveCurator);
        saveImage(broken, "https://storage/broken-1.jpg", 1);
        setCurationUserColumns(broken, goneCurator);
        jdbcTemplate.update("UPDATE curations SET reviewed_by = ? WHERE id = ?", toBytes(goneManager), toBytes(broken));
        setImageUserColumns(broken, goneCurator);

        // 정상 행: 현재 회원 값이라 그대로 남아야 한다
        UUID healthy = saveApprovedCuration(liveCurator);
        setCurationUserColumns(healthy, liveCurator);
        jdbcTemplate.update("UPDATE curations SET reviewed_by = ? WHERE id = ?", toBytes(liveManager), toBytes(healthy));

        runMigration("db/migration/V202611201100__replace_dangling_user_ids_on_curations.sql");

        UUID withdrawnCurator = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", goneCurator);
        UUID withdrawnManager = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", goneManager);
        for (String column : new String[]{"curator_id", "created_by", "updated_by", "deleted_by"}) {
            assertThat(curationColumn(broken, column)).as("curations.%s", column).isEqualTo(withdrawnCurator);
        }
        assertThat(curationColumn(broken, "reviewed_by")).isEqualTo(withdrawnManager);
        assertThat(imageColumn(broken, "created_by")).isEqualTo(withdrawnCurator);
        assertThat(imageColumn(broken, "updated_by")).isEqualTo(withdrawnCurator);

        assertThat(curationColumn(healthy, "curator_id")).isEqualTo(liveCurator);
        assertThat(curationColumn(healthy, "created_by")).isEqualTo(liveCurator);
        assertThat(curationColumn(healthy, "reviewed_by")).isEqualTo(liveManager);
    }

    @Test
    void 마이그레이션은_이미_탈퇴자_UUID인_값은_건드리지_않는다() {
        UUID liveCurator = saveUser(UserRole.CURATOR);
        UUID alreadyWithdrawn = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", UUID.randomUUID());
        UUID curationId = saveApprovedCuration(liveCurator);
        setCurationUserColumns(curationId, alreadyWithdrawn);

        runMigration("db/migration/V202611201100__replace_dangling_user_ids_on_curations.sql");

        assertThat(curationColumn(curationId, "curator_id")).isEqualTo(alreadyWithdrawn);
        assertThat(curationColumn(curationId, "created_by")).isEqualTo(alreadyWithdrawn);
    }


    // ===== 도우미 =====

    private void runMigration(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
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

    private UUID saveCuration(UUID curatorId) {
        return transactionTemplate.execute(status -> {
            Curation curation = Curation.builder()
                    .curatorId(curatorId)
                    .submittedProgramTitle("탈퇴 테스트 프로그램")
                    .submittedPlace("테스트 장소")
                    .tagline("한 줄 소개")
                    .content("큐레이션 본문")
                    .build();
            em.persist(curation);
            return curation.getId();
        });
    }

    private UUID saveApprovedCuration(UUID curatorId) {
        UUID curationId = saveCuration(curatorId);
        transactionTemplate.executeWithoutResult(status -> {
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
            em.find(Curation.class, curationId).approve(program, UUID.randomUUID());
        });
        return curationId;
    }

    private void softDelete(UUID curationId, UUID deletedBy) {
        transactionTemplate.executeWithoutResult(status -> em.find(Curation.class, curationId).softDelete(deletedBy));
    }

    private void saveImage(UUID curationId, String imageUrl, int displayOrder) {
        transactionTemplate.executeWithoutResult(status -> em.persist(CurationImage.builder()
                .curation(em.find(Curation.class, curationId))
                .imageUrl(imageUrl)
                .displayOrder(displayOrder)
                .build()));
    }

    private void setCurationUserColumns(UUID curationId, UUID userId) {
        jdbcTemplate.update(
                "UPDATE curations SET curator_id = ?, created_by = ?, updated_by = ?, reviewed_by = ?, deleted_by = ? WHERE id = ?",
                toBytes(userId), toBytes(userId), toBytes(userId), toBytes(userId), toBytes(userId), toBytes(curationId));
    }

    private void setImageUserColumns(UUID curationId, UUID userId) {
        jdbcTemplate.update(
                "UPDATE curation_images SET created_by = ?, updated_by = ? WHERE program_curation_id = ?",
                toBytes(userId), toBytes(userId), toBytes(curationId));
    }

    private boolean curationExists(UUID curationId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM curations WHERE id = ?", Integer.class, toBytes(curationId)) > 0;
    }

    private int imageCount(UUID curationId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM curation_images WHERE program_curation_id = ?", Integer.class, toBytes(curationId));
    }

    private UUID curationColumn(UUID curationId, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM curations WHERE id = ?", (rs, i) -> toUuid(rs.getBytes(1)), toBytes(curationId));
    }

    private UUID imageColumn(UUID curationId, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM curation_images WHERE program_curation_id = ? LIMIT 1",
                (rs, i) -> toUuid(rs.getBytes(1)), toBytes(curationId));
    }

    private static byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    private static UUID toUuid(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
