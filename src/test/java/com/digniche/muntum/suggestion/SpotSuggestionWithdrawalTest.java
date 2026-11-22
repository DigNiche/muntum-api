package com.digniche.muntum.suggestion;

import com.digniche.muntum.global.auditor.UserDisplayNames;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.suggestion.dto.request.SpotSuggestionRequest;
import com.digniche.muntum.suggestion.dto.request.SuggestionStatusUpdateRequest;
import com.digniche.muntum.suggestion.dto.response.SpotSuggestionResponse;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;
import com.digniche.muntum.suggestion.repository.SpotSuggestionRepository;
import com.digniche.muntum.suggestion.service.SpotSuggestionService;
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

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 제보 검토자(reviewed_by)·제보자(informer)가 FK 없는 UUID 칸이 된 뒤의 탈퇴 처리와 응답
 * - 실제 MySQL에 Flyway 마이그레이션을 적용한 스키마로 확인
 * - 탈퇴는 UserService.completeWithdrawal의 제보 칸 처리(탈퇴자 UUID 치환) + 회원 행 삭제로 재현한다
 * - 화면 문구: 탈퇴한 검토자 '문틈', 탈퇴한 제보자 '익명의 제보자'
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        SpotSuggestionService.class,
        UserDisplayResolver.class,
        SpotSuggestionWithdrawalTest.TestAuditConfig.class
})
class SpotSuggestionWithdrawalTest {

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
    SpotSuggestionService spotSuggestionService;

    @Autowired
    SpotSuggestionRepository spotSuggestionRepository;

    @Autowired
    EntityManager em;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM spot_suggestions");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 검토자_칸에는_FK도_인덱스도_없다() {
        Integer foreignKeys = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'spot_suggestions'
                  AND COLUMN_NAME = 'reviewed_by'
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, Integer.class);
        Integer indexes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'spot_suggestions'
                  AND COLUMN_NAME = 'reviewed_by'
                """, Integer.class);

        assertThat(foreignKeys).isZero();
        assertThat(indexes).isZero();
    }

    @Test
    void 살아_있는_검토자는_회원_id와_닉네임으로_나온다() {
        UUID managerId = saveUser(UserRole.MANAGER, "검토관리자");
        UUID audienceId = saveUser(UserRole.AUDIENCE, "제보자");
        UUID suggestionId = createAndApprove(audienceId, managerId);

        SpotSuggestionResponse detail = spotSuggestionService.getSpotSuggestion(suggestionId, managerId, UserRole.MANAGER);

        assertThat(detail.reviewerId()).isEqualTo(managerId);
        assertThat(detail.reviewerNickname()).isEqualTo("검토관리자");
    }

    @Test
    void 검토한_관리자가_탈퇴하면_검토자는_탈퇴자_UUID가_되고_응답에는_문틈으로_나온다() {
        UUID managerId = saveUser(UserRole.MANAGER, "탈퇴할관리자");
        UUID audienceId = saveUser(UserRole.AUDIENCE, "제보자2");
        UUID suggestionId = createAndApprove(audienceId, managerId);
        UUID withdrawnUuid = AuditorAwareImpl.toWithdrawnUserUuid("MANAGER", managerId);

        // 관리자 탈퇴: 검토자 칸을 탈퇴자 UUID로 바꾼 뒤 회원 행을 지운다 (FK가 없어 막히지 않는다)
        transactionTemplate.executeWithoutResult(status -> {
            spotSuggestionRepository.replaceReviewedByWith(managerId, withdrawnUuid);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(managerId));
        });

        byte[] storedReviewer = jdbcTemplate.queryForObject(
                "SELECT reviewed_by FROM spot_suggestions WHERE id = ?", byte[].class, toBytes(suggestionId));
        assertThat(storedReviewer).isEqualTo(toBytes(withdrawnUuid));

        SpotSuggestionResponse detail = spotSuggestionService.getSpotSuggestion(suggestionId, audienceId, UserRole.AUDIENCE);
        assertThat(detail.reviewerId()).isNull();
        assertThat(detail.reviewerNickname()).isEqualTo(UserDisplayNames.MUNTUM);

        SpotSuggestionResponse inList = spotSuggestionService.getSpotSuggestionList(null, 0, 20).content().get(0);
        assertThat(inList.reviewerId()).isNull();
        assertThat(inList.reviewerNickname()).isEqualTo(UserDisplayNames.MUNTUM);
    }

    @Test
    void 검토_전_제보는_검토자가_비어_있다() {
        UUID audienceId = saveUser(UserRole.AUDIENCE, "제보자3");
        UUID suggestionId = spotSuggestionService.createSpotSuggestion(audienceId, request()).id();

        SpotSuggestionResponse inList = spotSuggestionService.getMySuggestionList(audienceId, 0, 20).content().get(0);

        assertThat(inList.id()).isEqualTo(suggestionId);
        assertThat(inList.reviewerId()).isNull();
        assertThat(inList.reviewerNickname()).isNull();
    }

    @Test
    void 제보자_칸에는_FK가_없고_내_제보_목록용_인덱스는_남아_있다() {
        Integer foreignKeys = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'spot_suggestions'
                  AND COLUMN_NAME = 'informer'
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, Integer.class);
        Integer index = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'spot_suggestions'
                  AND INDEX_NAME = 'idx_suggestions_user_id'
                  AND COLUMN_NAME = 'informer'
                """, Integer.class);

        assertThat(foreignKeys).isZero();
        assertThat(index).isEqualTo(1);
    }

    @Test
    void 살아_있는_제보자는_회원_id와_닉네임으로_나오고_내_제보_목록과_본인_확인이_동작한다() {
        UUID audienceId = saveUser(UserRole.AUDIENCE, "살아있는제보자");
        UUID otherId = saveUser(UserRole.AUDIENCE, "다른회원");
        UUID suggestionId = spotSuggestionService.createSpotSuggestion(audienceId, request()).id();

        SpotSuggestionResponse mine = spotSuggestionService.getMySuggestionList(audienceId, 0, 20).content().get(0);
        assertThat(mine.id()).isEqualTo(suggestionId);
        assertThat(mine.informerId()).isEqualTo(audienceId);
        assertThat(mine.informerNickname()).isEqualTo("살아있는제보자");

        assertThat(spotSuggestionService.getMySuggestionList(otherId, 0, 20).content()).isEmpty();
        assertThatThrownBy(() -> spotSuggestionService.getSpotSuggestion(suggestionId, otherId, UserRole.AUDIENCE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 제보한_관람객이_탈퇴하면_제보자는_탈퇴자_UUID가_되고_응답에는_익명의_제보자로_나온다() {
        UUID audienceId = saveUser(UserRole.AUDIENCE, "탈퇴할제보자");
        UUID managerId = saveUser(UserRole.MANAGER, "조회관리자");
        UUID suggestionId = spotSuggestionService.createSpotSuggestion(audienceId, request()).id();
        UUID withdrawnUuid = AuditorAwareImpl.toWithdrawnUserUuid("AUDIENCE", audienceId);

        // 관람객 탈퇴: 제보자 칸을 탈퇴자 UUID로 바꾼 뒤 회원 행을 지운다 (FK가 없어 막히지 않는다)
        transactionTemplate.executeWithoutResult(status -> {
            spotSuggestionRepository.replaceInformerWith(audienceId, withdrawnUuid);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", toBytes(audienceId));
        });

        byte[] storedInformer = jdbcTemplate.queryForObject(
                "SELECT informer FROM spot_suggestions WHERE id = ?", byte[].class, toBytes(suggestionId));
        assertThat(storedInformer).isEqualTo(toBytes(withdrawnUuid));

        SpotSuggestionResponse detail = spotSuggestionService.getSpotSuggestion(suggestionId, managerId, UserRole.MANAGER);
        assertThat(detail.informerId()).isNull();
        assertThat(detail.informerNickname()).isEqualTo(UserDisplayNames.ANONYMOUS_INFORMER);

        SpotSuggestionResponse inList = spotSuggestionService.getSpotSuggestionList(null, 0, 20).content().get(0);
        assertThat(inList.informerId()).isNull();
        assertThat(inList.informerNickname()).isEqualTo(UserDisplayNames.ANONYMOUS_INFORMER);
    }

    private UUID createAndApprove(UUID informerId, UUID managerId) {
        UUID suggestionId = spotSuggestionService.createSpotSuggestion(informerId, request()).id();
        spotSuggestionService.updateSuggestionStatus(
                suggestionId, managerId, new SuggestionStatusUpdateRequest(SuggestionStatus.APPROVED));
        return suggestionId;
    }

    private static SpotSuggestionRequest request() {
        return new SpotSuggestionRequest("검토자 테스트 프로그램", "서울시 테스트구", "테스트 제보 사유");
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

    private static byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}
