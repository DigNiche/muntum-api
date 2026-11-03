package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.dto.response.CurationListResponse;
import com.digniche.muntum.curation.dto.response.ManagerCurationListResponse;
import com.digniche.muntum.curation.dto.response.PublicCurationDetailResponse;
import com.digniche.muntum.curation.dto.response.PublicCurationSummaryResponse;
import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.service.CurationImageService;
import com.digniche.muntum.curation.service.CurationService;
import com.digniche.muntum.curation.service.PublicCurationQueryService;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.storage.ImageStorageService;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.service.ProgramService;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.service.CuratorProfileService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 큐레이션 응답의 like 필드(좋아요 수와 내 상태)
 * - 어떤 큐레이션을 보여줄지는 화면별 조회 API가 정하고, like 필드는 프로그램·게시 상태와 무관하게 DB 이력 그대로 붙는다
 * - 실제 MySQL 컨테이너에 Flyway 마이그레이션을 적용한 뒤 공개 큐레이션 조회, 내 큐레이션 조회, 관리자 조회 서비스를 실행
 * - 쿼리 수는 이 기능이 약속하는 성능 계약이므로 Hibernate 통계로 확인한다 (항목 1개와 20개의 쿼리 수가 같다)
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import({
        CurationLikeService.class,
        CurationLikeQueryService.class,
        PublicCurationQueryService.class,
        CurationService.class,
        CurationImageService.class,
        CuratorProfileService.class,
        CurationLikeResponseTest.TestAuditConfig.class
})
class CurationLikeResponseTest {

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

    // 조회 경로는 스토리지를 쓰지 않는다. 컨텍스트를 띄우기 위해서만 둔다
    @MockitoBean
    ImageStorageService imageStorageService;

    // 큐레이션 승인 경로에서만 쓴다. 이 테스트의 조회 경로는 부르지 않는다
    @MockitoBean
    ProgramService programService;

    @Autowired
    CurationLikeService curationLikeService;

    @Autowired
    PublicCurationQueryService publicCurationQueryService;

    @Autowired
    CurationService curationService;

    @Autowired
    TestEntityManager em;

    @Test
    void 목록에서_큐레이션마다_좋아요_수와_내_상태가_한_번에_맞게_나온다() {
        User me = saveUser(UserRole.AUDIENCE);
        User other = saveUser(UserRole.AUDIENCE);
        Program program = saveProgram(ProgramStatus.ACTIVE);
        Curation likedByBoth = savePublishedCuration(program);
        Curation likedByOther = savePublishedCuration(program);
        Curation noLike = savePublishedCuration(program);
        curationLikeService.like(likedByBoth.getId(), me.getId(), UserRole.AUDIENCE);
        curationLikeService.like(likedByBoth.getId(), other.getId(), UserRole.AUDIENCE);
        curationLikeService.like(likedByOther.getId(), other.getId(), UserRole.AUDIENCE);

        Map<UUID, CurationLikeResponse> likes =
                likesById(publicCurationQueryService.getAllSummaries(program.getId(), me.getId()));

        assertThat(likes).containsOnly(
                Map.entry(likedByBoth.getId(), new CurationLikeResponse(2, true)),
                Map.entry(likedByOther.getId(), new CurationLikeResponse(1, false)),
                Map.entry(noLike.getId(), new CurationLikeResponse(0, false))
        );
    }

    @Test
    void 비로그인이면_좋아요_수는_보이고_내_상태는_false다() {
        User liker = saveUser(UserRole.AUDIENCE);
        Program program = saveProgram(ProgramStatus.ACTIVE);
        Curation curation = savePublishedCuration(program);
        curationLikeService.like(curation.getId(), liker.getId(), UserRole.AUDIENCE);

        PublicCurationDetailResponse detail = publicCurationQueryService.getCuration(program.getId(), curation.getId(), null);
        List<PublicCurationSummaryResponse> page =
                publicCurationQueryService.getCurations(program.getId(), null, 0, 20).content();

        assertThat(detail.like()).isEqualTo(new CurationLikeResponse(1, false));
        assertThat(page).extracting(PublicCurationSummaryResponse::like)
                .containsExactly(new CurationLikeResponse(1, false));
    }

    @Test
    void 비공개였다가_다시_공개된_큐레이션의_좋아요_수와_내_상태가_그대로다() {
        User me = saveUser(UserRole.AUDIENCE);
        Program program = saveProgram(ProgramStatus.ACTIVE);
        Curation curation = savePublishedCuration(program);
        curationLikeService.like(curation.getId(), me.getId(), UserRole.AUDIENCE);

        // 좋아요 수 갱신 쿼리가 영속성 컨텍스트를 비우므로 다시 읽어 상태를 바꾼다
        Curation stored = em.find(Curation.class, curation.getId());
        stored.requestChanges(UUID.randomUUID(), "수정 요청 사유", CurationPublicationStatus.UNPUBLISHED);
        stored.resubmit();
        stored.approve(em.find(Program.class, program.getId()), UUID.randomUUID());
        em.flush();

        assertThat(publicCurationQueryService.getCuration(program.getId(), curation.getId(), me.getId()).like())
                .isEqualTo(new CurationLikeResponse(1, true));
    }

    @Test
    void 공개_상세는_공개_기준_밖이면_기존과_같은_에러를_준다() {
        Program program = saveProgram(ProgramStatus.ACTIVE);
        Program anotherProgram = saveProgram(ProgramStatus.ACTIVE);
        Program hiddenProgram = saveProgram(ProgramStatus.HIDDEN);

        Curation unpublished = savePublishedCuration(program);
        unpublished.requestChanges(UUID.randomUUID(), "수정 요청 사유", CurationPublicationStatus.UNPUBLISHED);
        Curation deleted = savePublishedCuration(program);
        deleted.softDelete(deleted.getCuratorId());
        Curation ofAnotherProgram = savePublishedCuration(anotherProgram);
        Curation ofHiddenProgram = savePublishedCuration(hiddenProgram);
        em.flush();

        assertErrorCode(() -> publicCurationQueryService.getCuration(program.getId(), unpublished.getId(), null),
                ErrorCode.CURATION_NOT_FOUND);
        assertErrorCode(() -> publicCurationQueryService.getCuration(program.getId(), deleted.getId(), null),
                ErrorCode.CURATION_NOT_FOUND);
        assertErrorCode(() -> publicCurationQueryService.getCuration(program.getId(), ofAnotherProgram.getId(), null),
                ErrorCode.CURATION_NOT_FOUND);
        assertErrorCode(() -> publicCurationQueryService.getCuration(hiddenProgram.getId(), ofHiddenProgram.getId(), null),
                ErrorCode.PROGRAM_NOT_FOUND);
    }

    @Test
    void 프로그램별_큐레이션_목록의_쿼리_수는_큐레이션_수와_무관하다() {
        User me = saveUser(UserRole.AUDIENCE);
        Program one = programWithLikedCurations(1, me);
        Program twenty = programWithLikedCurations(20, me);

        // 페이지가 다 차지 않으면 Spring Data가 COUNT 쿼리를 생략하므로, 두 경우 모두 다 차지 않는 크기로 조회한다
        long oneQueries = countQueries(() -> publicCurationQueryService.getCurations(one.getId(), me.getId(), 0, 50));
        long twentyQueries = countQueries(() -> publicCurationQueryService.getCurations(twenty.getId(), me.getId(), 0, 50));

        assertThat(twentyQueries).isEqualTo(oneQueries);
    }

    @Test
    void 프로그램_상세의_큐레이션_목록의_쿼리_수는_큐레이션_수와_무관하다() {
        User me = saveUser(UserRole.AUDIENCE);
        Program one = programWithLikedCurations(1, me);
        Program twenty = programWithLikedCurations(20, me);

        long oneQueries = countQueries(() -> publicCurationQueryService.getAllSummaries(one.getId(), me.getId()));
        long twentyQueries = countQueries(() -> publicCurationQueryService.getAllSummaries(twenty.getId(), me.getId()));

        assertThat(twentyQueries).isEqualTo(oneQueries);
    }

    @Test
    void 내_큐레이션은_프로그램이_숨김_삭제되거나_비공개가_되어도_좋아요_수와_내_상태가_그대로_나온다() {
        User curator = saveUser(UserRole.CURATOR);
        User audience = saveUser(UserRole.AUDIENCE);
        Program toHide = saveProgram(ProgramStatus.ACTIVE);
        Program toDelete = saveProgram(ProgramStatus.ACTIVE);
        Program program = saveProgram(ProgramStatus.ACTIVE);
        Program anotherProgram = saveProgram(ProgramStatus.ACTIVE); // 큐레이터 1명은 프로그램 1개에 큐레이션 1개만 쓸 수 있다
        Curation ofHiddenProgram = savePublishedCuration(toHide, curator.getId());
        Curation ofDeletedProgram = savePublishedCuration(toDelete, curator.getId());
        Curation unpublished = savePublishedCuration(program, curator.getId());
        Curation deleted = savePublishedCuration(anotherProgram, curator.getId());
        for (Curation curation : List.of(ofHiddenProgram, ofDeletedProgram, unpublished, deleted)) {
            curationLikeService.like(curation.getId(), audience.getId(), UserRole.AUDIENCE);
        }
        curationLikeService.like(ofHiddenProgram.getId(), curator.getId(), UserRole.CURATOR);

        // 좋아요 수 갱신 쿼리가 영속성 컨텍스트를 비우므로 다시 읽어 상태를 바꾼다
        em.find(Program.class, toHide.getId()).updateStatus(ProgramStatus.HIDDEN);
        em.find(Program.class, toDelete.getId()).softDelete(UUID.randomUUID());
        em.find(Curation.class, unpublished.getId())
                .requestChanges(UUID.randomUUID(), "수정 요청 사유", CurationPublicationStatus.UNPUBLISHED);
        em.find(Curation.class, deleted.getId()).softDelete(curator.getId());
        em.flush();
        em.clear();

        Map<UUID, CurationLikeResponse> likes = curationService.getMyCurations(curator.getId(), null, 0, 20).content()
                .stream()
                .collect(Collectors.toMap(CurationListResponse::id, CurationListResponse::like));

        // 삭제된 큐레이션은 내 목록에서도 빠지고, 나머지는 프로그램·게시 상태와 무관하게 좋아요 이력이 그대로 나온다
        assertThat(likes).containsOnly(
                Map.entry(ofHiddenProgram.getId(), new CurationLikeResponse(2, true)),
                Map.entry(ofDeletedProgram.getId(), new CurationLikeResponse(1, false)),
                Map.entry(unpublished.getId(), new CurationLikeResponse(1, false))
        );
        assertThat(curationService.getMyCuration(ofHiddenProgram.getId(), curator.getId()).like())
                .isEqualTo(new CurationLikeResponse(2, true));
        assertThat(curationService.getMyCuration(ofDeletedProgram.getId(), curator.getId()).like())
                .isEqualTo(new CurationLikeResponse(1, false));
        assertThat(curationService.getMyCuration(unpublished.getId(), curator.getId()).like())
                .isEqualTo(new CurationLikeResponse(1, false));
    }

    @Test
    void 관리자_목록에는_게시_상태와_무관하게_좋아요_수가_나오고_내_상태는_false다() {
        User audience = saveUser(UserRole.AUDIENCE);
        Program program = saveProgram(ProgramStatus.ACTIVE);
        Curation unpublished = savePublishedCuration(program);
        Curation stillPublished = savePublishedCuration(program);
        curationLikeService.like(unpublished.getId(), audience.getId(), UserRole.AUDIENCE);
        curationLikeService.like(stillPublished.getId(), audience.getId(), UserRole.AUDIENCE);

        em.find(Curation.class, unpublished.getId())
                .requestChanges(UUID.randomUUID(), "수정 요청 사유", CurationPublicationStatus.UNPUBLISHED);
        em.find(Curation.class, stillPublished.getId())
                .requestChanges(UUID.randomUUID(), "수정 요청 사유", CurationPublicationStatus.PUBLISHED);
        em.flush();
        em.clear();

        List<ManagerCurationListResponse> responses =
                curationService.getCurationsForManager(CurationStatus.CHANGES_REQUESTED, 0, 20).content();

        assertThat(responses).extracting(ManagerCurationListResponse::id, ManagerCurationListResponse::like)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(unpublished.getId(), new CurationLikeResponse(1, false)),
                        org.assertj.core.groups.Tuple.tuple(stillPublished.getId(), new CurationLikeResponse(1, false))
                );
    }

    // 큐레이터가 모두 다르고, 절반은 내가 좋아요를 누른 큐레이션들
    private Program programWithLikedCurations(int size, User me) {
        Program program = saveProgram(ProgramStatus.ACTIVE);
        List<Curation> curations = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            curations.add(savePublishedCuration(program));
        }
        for (int i = 0; i < size; i += 2) {
            curationLikeService.like(curations.get(i).getId(), me.getId(), UserRole.AUDIENCE);
        }
        return program;
    }

    private long countQueries(Runnable call) {
        em.clear();
        Statistics statistics = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        call.run();
        return statistics.getPrepareStatementCount();
    }

    private static Map<UUID, CurationLikeResponse> likesById(List<PublicCurationSummaryResponse> summaries) {
        return summaries.stream()
                .collect(Collectors.toMap(PublicCurationSummaryResponse::id, PublicCurationSummaryResponse::like));
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

    private Program saveProgram(ProgramStatus programStatus) {
        Program program = em.persist(Program.builder()
                .title("좋아요 응답 테스트 프로그램")
                .programType(ProgramType.EXHIBITION)
                .description("좋아요 응답 테스트 프로그램 소개")
                .free(true)
                .venueName("테스트 장소")
                .address("서울시 테스트구")
                .latitude(new BigDecimal("37.5"))
                .longitude(new BigDecimal("127.0"))
                .build());
        program.updateStatus(programStatus);
        em.flush();
        return program;
    }

    private Curation savePublishedCuration(Program program) {
        return savePublishedCuration(program, saveUser(UserRole.CURATOR).getId());
    }

    private Curation savePublishedCuration(Program program, UUID curatorId) {
        Curation curation = em.persist(Curation.builder()
                .curatorId(curatorId)
                .submittedProgramTitle("좋아요 응답 테스트 프로그램")
                .submittedPlace("테스트 장소")
                .tagline("한 줄 소개")
                .content("큐레이션 본문")
                .build());
        curation.approve(em.find(Program.class, program.getId()), UUID.randomUUID());
        em.flush();
        return curation;
    }
}
