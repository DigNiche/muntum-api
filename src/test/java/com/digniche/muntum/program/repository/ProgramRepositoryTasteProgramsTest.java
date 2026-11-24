package com.digniche.muntum.program.repository;

import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.entity.KeywordType;
import com.digniche.muntum.keyword.entity.UserKeyword;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramKeyword;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.entity.ReservationType;
import com.digniche.muntum.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 내 취향 모아보기 (로그인 사용자 탐색 탭 섹션·전체보기, GET /api/v1/taste/me)
 * - 내 활성 취향 키워드 중 하나라도 가진 운영 중 프로그램을, 겹치는 키워드 수가 많은 순 → 최신 등록순으로
 * - 기존 쿼리의 동작을 고정하는 테스트 (서비스는 ACTIVE만 넘긴다)
 * - 실제 MySQL 컨테이너에 Flyway 마이그레이션을 적용한 뒤 저장소 쿼리를 실행
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import(ProgramRepositoryTasteProgramsTest.TestAuditConfig.class)
class ProgramRepositoryTasteProgramsTest {

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

    // 서비스(ProgramService.getTastePrograms)가 넘기는 값과 같게
    private static final List<ProgramStatus> ACTIVE_ONLY = List.of(ProgramStatus.ACTIVE);
    private static final List<ReservationType> NO_RESERVATION_TYPES = ReservationType.NO_RESERVATION_TYPES;
    private static final Pageable PAGE = PageRequest.of(0, 20, Sort.unsorted());

    @Autowired
    ProgramRepository programRepository;

    @Autowired
    UserKeywordRepository userKeywordRepository;

    @Autowired
    TestEntityManager em;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = em.persist(User.builder()
                .email("taste@taste-programs.test")
                .password("password")
                .build());
        userId = user.getId();

        Keyword mineA = keyword("내 키워드 A");
        Keyword mineB = keyword("내 키워드 B");
        Keyword mineInactive = keyword("내 비활성 키워드");
        Keyword other = keyword("관계없는 키워드");
        mineInactive.deactivate();

        for (Keyword keyword : List.of(mineA, mineB, mineInactive)) {
            em.persist(UserKeyword.builder().user(user).keyword(keyword).build());
        }

        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 12, 0);
        program("둘 다 겹침 오래됨", ProgramType.EXHIBITION, base.minusDays(9), mineA, mineB);
        program("A만 겹침 최신", ProgramType.EXHIBITION, base, mineA);
        program("B만 겹침", ProgramType.EXHIBITION, base.minusDays(2), mineB);
        program("A만 겹침 공연", ProgramType.PERFORMANCE, base.minusDays(1), mineA);

        // 목록에서 빠져야 하는 프로그램
        program("관계없는 키워드만", ProgramType.EXHIBITION, base, other);
        program("비활성 내 키워드만", ProgramType.EXHIBITION, base, mineInactive);
        program("종료된 A", ProgramType.EXHIBITION, base, mineA).updateStatus(ProgramStatus.ENDED);
        program("삭제된 A", ProgramType.EXHIBITION, base, mineA).softDelete(UUID.randomUUID());

        em.flush();
        em.clear();
    }

    @Test
    void 취향_키워드는_활성_키워드만_가져온다() {
        List<UUID> keywordIds = userKeywordRepository.findActiveKeywordIdsByUserId(userId);

        assertThat(keywordIds).hasSize(2);
    }

    @Test
    void 겹치는_키워드가_많은_순_그다음_최신순이고_종료_삭제_무관한_프로그램은_빠진다() {
        Page<Program> page = searchTaste(null);

        assertThat(titles(page)).containsExactly(
                "둘 다 겹침 오래됨", // 2개 겹침이 등록일과 상관없이 먼저
                "A만 겹침 최신",
                "A만 겹침 공연",
                "B만 겹침");
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    void 유형_칩을_고르면_그_유형만_나온다() {
        Page<Program> page = searchTaste(ProgramType.PERFORMANCE);

        assertThat(titles(page)).containsExactly("A만 겹침 공연");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    private Page<Program> searchTaste(ProgramType programType) {
        List<UUID> keywordIds = userKeywordRepository.findActiveKeywordIdsByUserId(userId);
        return programRepository.searchTasteProgramsByKeywordIds(
                ACTIVE_ONLY, keywordIds,
                null,
                null, NO_RESERVATION_TYPES,
                programType,
                null, null,
                PAGE);
    }

    private List<String> titles(Page<Program> page) {
        return page.getContent().stream().map(Program::getTitle).toList();
    }

    private Keyword keyword(String name) {
        return em.persist(Keyword.builder()
                .name(name)
                .type(KeywordType.THEME)
                .build());
    }

    // 등록 시각은 감사(Audit)가 현재 시각으로 채우므로, 순서를 고정하려고 저장 후 직접 바꾼다
    private Program program(String title, ProgramType type, LocalDateTime createdAt, Keyword... keywords) {
        Program program = em.persist(Program.builder()
                .title(title)
                .programType(type)
                .description(title + " 소개")
                .free(true)
                .venueName("테스트 장소")
                .address("서울시 테스트구")
                .latitude(new BigDecimal("37.5"))
                .longitude(new BigDecimal("127.0"))
                .build());
        for (Keyword keyword : keywords) {
            em.persist(ProgramKeyword.builder().program(program).keyword(keyword).build());
        }
        em.flush();
        em.getEntityManager()
                .createQuery("UPDATE Program p SET p.createdAt = :createdAt WHERE p.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", program.getId())
                .executeUpdate();
        return program;
    }
}
