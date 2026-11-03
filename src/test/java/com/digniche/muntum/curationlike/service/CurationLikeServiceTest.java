package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 큐레이션 좋아요 누르기·취소·조회
 * - 실제 MySQL 컨테이너에 Flyway 마이그레이션을 적용한 뒤 서비스를 실행
 * - 결과는 응답과 DB 상태(좋아요 행 수, curations.like_count)로만 확인
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import({CurationLikeService.class, CurationLikeServiceTest.TestAuditConfig.class})
class CurationLikeServiceTest {

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
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 좋아요를_두_번_눌러도_좋아요는_하나이고_수는_1이다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Curation curation = savePublishedCuration(ProgramStatus.ACTIVE);

        curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE);
        CurationLikeResponse response =
                curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.liked()).isTrue();
        assertThat(likeRowCount(curation.getId())).isEqualTo(1);
        assertThat(storedLikeCount(curation.getId())).isEqualTo(1);
    }

    @Test
    void 좋아요를_취소하고_다시_취소해도_좋아요는_없고_수는_0이다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Curation curation = savePublishedCuration(ProgramStatus.ACTIVE);
        curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE);

        curationLikeService.cancelLike(curation.getId(), audience.getId(), UserRole.AUDIENCE);
        // 이미 취소된 상태에서 같은 요청을 다시 보내도(재전송·연타) 성공하고 수가 그대로여야 한다
        CurationLikeResponse response =
                curationLikeService.cancelLike(curation.getId(), audience.getId(), UserRole.AUDIENCE);

        assertThat(response.count()).isZero();
        assertThat(response.liked()).isFalse();
        assertThat(likeRowCount(curation.getId())).isZero();
        assertThat(storedLikeCount(curation.getId())).isZero();
    }

    @Test
    void 비공개_큐레이션에는_큐레이터_본인도_좋아요를_누르거나_취소할_수_없다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Curation curation = savePublishedCuration(ProgramStatus.ACTIVE);
        curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE);
        // 좋아요 수 갱신 쿼리가 영속성 컨텍스트를 비우므로 다시 읽어 상태를 바꾼다
        em.find(Curation.class, curation.getId())
                .requestChanges(UUID.randomUUID(), "수정 요청 사유", CurationPublicationStatus.UNPUBLISHED);
        em.flush();

        assertErrorCode(() -> curationLikeService.like(curation.getId(), curation.getCuratorId(), UserRole.CURATOR),
                ErrorCode.UNPUBLISHED_CURATION_NOT_LIKEABLE);
        assertErrorCode(() -> curationLikeService.cancelLike(curation.getId(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.UNPUBLISHED_CURATION_NOT_LIKEABLE);
        // 비공개가 되어도 받은 좋아요는 그대로 남는다
        assertThat(likeRowCount(curation.getId())).isEqualTo(1);
        assertThat(storedLikeCount(curation.getId())).isEqualTo(1);
    }

    @Test
    void 없거나_삭제된_큐레이션에는_좋아요를_누르거나_취소할_수_없다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Curation curation = savePublishedCuration(ProgramStatus.ACTIVE);
        curation.softDelete(curation.getCuratorId());
        em.flush();

        assertErrorCode(() -> curationLikeService.like(UUID.randomUUID(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.CURATION_NOT_FOUND);
        assertErrorCode(() -> curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.CURATION_NOT_FOUND);
        assertErrorCode(() -> curationLikeService.cancelLike(curation.getId(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.CURATION_NOT_FOUND);
        assertThat(likeRowCount(curation.getId())).isZero();
    }

    @Test
    void 숨김_프로그램의_큐레이션에는_좋아요를_누르거나_취소할_수_없다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Curation curation = savePublishedCuration(ProgramStatus.HIDDEN);

        assertErrorCode(() -> curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.HIDDEN_PROGRAM_NOT_LIKEABLE);
        assertErrorCode(() -> curationLikeService.cancelLike(curation.getId(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.HIDDEN_PROGRAM_NOT_LIKEABLE);
        assertThat(likeRowCount(curation.getId())).isZero();
    }

    @Test
    void 삭제된_프로그램의_큐레이션에는_좋아요를_누르거나_취소할_수_없다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Curation curation = savePublishedCuration(ProgramStatus.ACTIVE);
        curation.getProgram().softDelete(UUID.randomUUID());
        em.flush();

        assertErrorCode(() -> curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.DELETED_PROGRAM_NOT_LIKEABLE);
        assertErrorCode(() -> curationLikeService.cancelLike(curation.getId(), audience.getId(), UserRole.AUDIENCE),
                ErrorCode.DELETED_PROGRAM_NOT_LIKEABLE);
        assertThat(likeRowCount(curation.getId())).isZero();
    }

    @Test
    void 종료된_프로그램의_큐레이션에는_좋아요를_누를_수_있다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Curation curation = savePublishedCuration(ProgramStatus.ENDED);

        CurationLikeResponse response =
                curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE);

        assertThat(response.count()).isEqualTo(1);
        assertThat(likeRowCount(curation.getId())).isEqualTo(1);
    }

    @Test
    void 관리자는_좋아요를_누르거나_취소할_수_없다() {
        User manager = saveUser(UserRole.MANAGER);
        Curation curation = savePublishedCuration(ProgramStatus.ACTIVE);

        assertThatThrownBy(() -> curationLikeService.like(curation.getId(), manager.getId(), UserRole.MANAGER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> curationLikeService.cancelLike(curation.getId(), manager.getId(), UserRole.MANAGER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(likeRowCount(curation.getId())).isZero();
        assertThat(storedLikeCount(curation.getId())).isZero();
    }

    @Test
    void 큐레이터는_자기_큐레이션에도_좋아요를_누를_수_있다() {
        Curation curation = savePublishedCuration(ProgramStatus.ACTIVE);

        CurationLikeResponse response =
                curationLikeService.like(curation.getId(), curation.getCuratorId(), UserRole.CURATOR);

        assertThat(response.count()).isEqualTo(1);
        assertThat(likeRowCount(curation.getId())).isEqualTo(1);
    }

    private void assertErrorCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(errorCode);
    }

    private User saveUser(UserRole role) {
        User user = em.persist(User.builder()
                .email(UUID.randomUUID() + "@test.com")
                .role(role)
                .build());
        em.flush();
        return user;
    }

    private Curation savePublishedCuration(ProgramStatus programStatus) {
        Program program = em.persist(Program.builder()
                .title("좋아요 테스트 프로그램")
                .programType(ProgramType.EXHIBITION)
                .description("좋아요 테스트 프로그램 소개")
                .free(true)
                .venueName("테스트 장소")
                .address("서울시 테스트구")
                .latitude(new BigDecimal("37.5"))
                .longitude(new BigDecimal("127.0"))
                .build());
        program.updateStatus(programStatus);

        Curation curation = em.persist(Curation.builder()
                .curatorId(saveUser(UserRole.CURATOR).getId())
                .submittedProgramTitle("좋아요 테스트 프로그램")
                .submittedPlace("테스트 장소")
                .tagline("한 줄 소개")
                .content("큐레이션 본문")
                .build());
        curation.approve(program, UUID.randomUUID());
        em.flush();
        return curation;
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
