package com.digniche.muntum.curator;

import com.digniche.muntum.curator.dto.request.CuratorApplicationCreateRequest;
import com.digniche.muntum.curator.dto.request.CuratorApplicationStatusUpdateRequest;
import com.digniche.muntum.curator.dto.response.CuratorApplicationCardResponse;
import com.digniche.muntum.curator.dto.response.CuratorApplicationResponse;
import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;
import com.digniche.muntum.curator.repository.CuratorApplicationRepository;
import com.digniche.muntum.curator.service.CuratorApplicationService;
import com.digniche.muntum.global.auditor.UserDisplayNames;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.user.dto.response.UserProfileResponse;
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
import org.springframework.data.domain.PageRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 큐레이터 지원서 탈퇴 처리와 응답
 * - 실제 MySQL에 Flyway 마이그레이션을 적용한 스키마로 확인
 * - 탈퇴는 UserService.completeWithdrawal의 지원서 처리(대기 지원서 삭제 → 역할별 탈퇴자 UUID 치환) + 회원 행 삭제로 재현한다
 * - 승인·반려로 검토가 확정된 지원서만 남기고, 대기 지원서는 지운다
 * - 화면 문구: 탈퇴한 지원자는 탈퇴 전 역할 문구(익명의 큐레이터/관람객), 탈퇴한 검토자는 '문틈'
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        CuratorApplicationService.class,
        UserDisplayResolver.class,
        CuratorApplicationWithdrawalTest.TestAuditConfig.class
})
class CuratorApplicationWithdrawalTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.45")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci")
            .withEnv("TZ", "Asia/Seoul");

    // 감사 칸(created_by/updated_by)에 "지금 요청한 회원"이 들어가도록 테스트에서 바꿀 수 있게 둔다
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
    CuratorApplicationService curatorApplicationService;
    @Autowired
    CuratorApplicationRepository curatorApplicationRepository;
    @Autowired
    EntityManager em;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM curator_applications");
        jdbcTemplate.update("DELETE FROM users");
    }


    // ===== 스키마 =====

    @Test
    void 지원자_칸에는_FK가_없고_내_지원_내역용_인덱스는_남아_있다() {
        Integer foreignKeys = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'curator_applications'
                  AND COLUMN_NAME = 'applicant_id'
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, Integer.class);
        Integer index = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'curator_applications'
                  AND INDEX_NAME = 'idx_curator_applications_applicant_id'
                  AND COLUMN_NAME = 'applicant_id'
                """, Integer.class);

        assertThat(foreignKeys).isZero();
        assertThat(index).isEqualTo(1);
    }


    // ===== 현재 회원 표시 =====

    @Test
    void 현재_회원인_지원자와_검토자는_회원_정보로_나온다() {
        UUID managerId = saveUser(UserRole.MANAGER, "검토관리자");
        UUID audienceId = saveUser(UserRole.AUDIENCE, "지원관람객");
        UUID applicationId = apply(audienceId);
        review(applicationId, managerId, CuratorApplicationStatus.APPROVED);

        CuratorApplicationResponse detail = curatorApplicationService.getApplication(applicationId, managerId, UserRole.MANAGER);

        assertThat(detail.applicant().userId()).isEqualTo(audienceId);
        assertThat(detail.applicant().nickname()).isEqualTo("지원관람객");
        assertThat(detail.applicant().email()).isNotNull();
        assertThat(detail.applicant().role()).isEqualTo("CURATOR");
        assertThat(detail.reviewer().userId()).isEqualTo(managerId);
        assertThat(detail.reviewer().nickname()).isEqualTo("검토관리자");
        assertThat(detail.reviewer().email()).isNotNull();
    }


    // ===== 지원자 탈퇴 =====

    @Test
    void 반려_후_재지원해_승인된_큐레이터가_탈퇴하면_두_지원서_모두_남고_익명의_큐레이터로_나온다() {
        UUID managerId = saveUser(UserRole.MANAGER, "관리자");
        UUID applicantId = saveUser(UserRole.AUDIENCE, "재지원자");
        UUID rejected = apply(applicantId);
        review(rejected, managerId, CuratorApplicationStatus.REJECTED);
        UUID approved = apply(applicantId);
        review(approved, managerId, CuratorApplicationStatus.APPROVED);
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("CURATOR", applicantId);

        withdrawApplicant(applicantId, withdrawn);

        assertThat(applicationIds()).containsExactlyInAnyOrder(rejected, approved);
        for (UUID applicationId : List.of(rejected, approved)) {
            assertThat(column(applicationId, "applicant_id")).isEqualTo(withdrawn);
            assertThat(column(applicationId, "created_by")).isEqualTo(withdrawn);
            // 검토(관리자)가 마지막 수정이라 수정자는 관리자 그대로
            assertThat(column(applicationId, "updated_by")).isEqualTo(managerId);
            assertThat(column(applicationId, "reviewed_by")).isEqualTo(managerId);
        }

        UserProfileResponse inDetail = curatorApplicationService.getApplication(approved, managerId, UserRole.MANAGER).applicant();
        assertWithdrawnApplicant(inDetail, UserDisplayNames.ANONYMOUS_CURATOR, "CURATOR");

        List<CuratorApplicationCardResponse> cards =
                curatorApplicationService.getAllApplications(null, PageRequest.of(0, 20)).content();
        assertThat(cards).hasSize(2);
        cards.forEach(card -> assertWithdrawnApplicant(card.applicant(), UserDisplayNames.ANONYMOUS_CURATOR, "CURATOR"));
    }

    @Test
    void 대기_중에_탈퇴하면_그_지원서는_지워지고_다른_사람의_대기_지원서는_남는다() {
        UUID applicantId = saveUser(UserRole.AUDIENCE, "대기중탈퇴");
        UUID otherId = saveUser(UserRole.AUDIENCE, "다른지원자");
        UUID pending = apply(applicantId);
        UUID othersPending = apply(otherId);

        withdrawApplicant(applicantId, AuditorAwareImpl.toWithdrawnUserUuid("AUDIENCE", applicantId));

        assertThat(applicationIds()).containsExactly(othersPending);
        assertThat(applicationIds()).doesNotContain(pending);
    }

    @Test
    void 반려된_관람객이_탈퇴하면_지원서는_남고_수정한_적_있으면_수정자도_바뀌며_익명의_관람객으로_나온다() {
        UUID managerId = saveUser(UserRole.MANAGER, "관리자2");
        UUID applicantId = saveUser(UserRole.AUDIENCE, "반려관람객");
        UUID rejected = apply(applicantId);
        // 대기 중 본인이 수정하면 수정자는 지원자
        editAsApplicant(rejected, applicantId);
        assertThat(column(rejected, "updated_by")).isEqualTo(applicantId);
        review(rejected, managerId, CuratorApplicationStatus.REJECTED);
        // 검토하면 수정자가 관리자로 바뀌므로, 수정자 칸 치환을 확인하려고 지원자로 되돌려 둔다
        setColumn(rejected, "updated_by", applicantId);
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("AUDIENCE", applicantId);

        withdrawApplicant(applicantId, withdrawn);

        assertThat(applicationIds()).containsExactly(rejected);
        assertThat(column(rejected, "applicant_id")).isEqualTo(withdrawn);
        assertThat(column(rejected, "created_by")).isEqualTo(withdrawn);
        assertThat(column(rejected, "updated_by")).isEqualTo(withdrawn);

        UserProfileResponse applicant = curatorApplicationService.getApplication(rejected, managerId, UserRole.MANAGER).applicant();
        assertWithdrawnApplicant(applicant, UserDisplayNames.ANONYMOUS_AUDIENCE, "AUDIENCE");
    }


    // ===== 검토자 탈퇴 =====

    @Test
    void 검토한_관리자가_탈퇴하면_검토자와_수정자는_탈퇴자_UUID가_되고_검토자는_문틈으로_이메일_없이_나온다() {
        UUID managerId = saveUser(UserRole.MANAGER, "탈퇴할관리자");
        UUID viewerId = saveUser(UserRole.MANAGER, "조회관리자");
        UUID applicantId = saveUser(UserRole.AUDIENCE, "지원자");
        UUID applicationId = apply(applicantId);
        review(applicationId, managerId, CuratorApplicationStatus.APPROVED);
        UUID withdrawn = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", managerId);

        // 관리자 탈퇴: 지원서의 수정자·검토자만 바꾼다 (지원은 관람객만 하므로 지원자·생성자는 해당 없음)
        transactionTemplate.executeWithoutResult(status -> {
            curatorApplicationRepository.deleteByApplicantIdAndStatus(managerId, CuratorApplicationStatus.PENDING);
            curatorApplicationRepository.replaceUpdatedByWith(managerId, withdrawn);
            curatorApplicationRepository.replaceReviewedByWith(managerId, withdrawn);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(managerId));
        });

        assertThat(column(applicationId, "reviewed_by")).isEqualTo(withdrawn);
        assertThat(column(applicationId, "updated_by")).isEqualTo(withdrawn);
        assertThat(column(applicationId, "applicant_id")).isEqualTo(applicantId);

        CuratorApplicationResponse detail = curatorApplicationService.getApplication(applicationId, viewerId, UserRole.MANAGER);
        assertThat(detail.reviewer().userId()).isNull();
        assertThat(detail.reviewer().nickname()).isEqualTo(UserDisplayNames.MUNTUM);
        assertThat(detail.reviewer().email()).isNull();
        assertThat(detail.applicant().userId()).isEqualTo(applicantId);
    }


    // ===== helpers =====

    // UserService.completeWithdrawal의 큐레이터·관람객 지원서 처리와 같은 순서
    private void withdrawApplicant(UUID userId, UUID withdrawnUuid) {
        transactionTemplate.executeWithoutResult(status -> {
            curatorApplicationRepository.deleteByApplicantIdAndStatus(userId, CuratorApplicationStatus.PENDING);
            curatorApplicationRepository.replaceApplicantIdWith(userId, withdrawnUuid);
            curatorApplicationRepository.replaceCreatedByWith(userId, withdrawnUuid);
            curatorApplicationRepository.replaceUpdatedByWith(userId, withdrawnUuid);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(userId));
        });
    }

    private UUID apply(UUID applicantId) {
        CURRENT_AUDITOR.set(applicantId);
        return curatorApplicationService.createCuratorApplication(applicantId, request("지원 프로그램")).id();
    }

    private void editAsApplicant(UUID applicationId, UUID applicantId) {
        CURRENT_AUDITOR.set(applicantId);
        curatorApplicationService.updateCuratorApplication(applicationId, applicantId, request("수정한 프로그램"));
    }

    private void review(UUID applicationId, UUID managerId, CuratorApplicationStatus status) {
        CURRENT_AUDITOR.set(managerId);
        CuratorApplicationRejectReason reason =
                status == CuratorApplicationStatus.REJECTED ? CuratorApplicationRejectReason.INSUFFICIENT_INFO : null;
        curatorApplicationService.evaluateApplication(applicationId, managerId, new CuratorApplicationStatusUpdateRequest(status, reason));
    }

    private static CuratorApplicationCreateRequest request(String programName) {
        return new CuratorApplicationCreateRequest(programName, "한 줄 소개", "큐레이션 본문");
    }

    private static void assertWithdrawnApplicant(UserProfileResponse applicant, String nickname, String role) {
        assertThat(applicant.userId()).isNull();
        assertThat(applicant.email()).isNull();
        assertThat(applicant.profileImageUrl()).isNull();
        assertThat(applicant.status()).isNull();
        assertThat(applicant.joinedAt()).isNull();
        assertThat(applicant.nickname()).isEqualTo(nickname);
        assertThat(applicant.role()).isEqualTo(role);
    }

    private UUID saveUser(UserRole role, String nickname) {
        return transactionTemplate.execute(status -> {
            User user = User.builder()
                    .email(UUID.randomUUID() + "@test.com")
                    .role(role)
                    .build();
            user.updateNickname(nickname);
            em.persist(user);
            return user.getId();
        });
    }

    private List<UUID> applicationIds() {
        return jdbcTemplate.queryForList("SELECT id FROM curator_applications", byte[].class)
                .stream().map(CuratorApplicationWithdrawalTest::toUuid).toList();
    }

    private UUID column(UUID applicationId, String column) {
        byte[] value = jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM curator_applications WHERE id = ?", byte[].class, toBytes(applicationId));
        return value == null ? null : toUuid(value);
    }

    private void setColumn(UUID applicationId, String column, UUID value) {
        jdbcTemplate.update("UPDATE curator_applications SET " + column + " = ? WHERE id = ?", toBytes(value), toBytes(applicationId));
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
