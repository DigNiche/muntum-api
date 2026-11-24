package com.digniche.muntum.program.repository;

import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.entity.KeywordType;
import com.digniche.muntum.keyword.entity.UserKeyword;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramKeyword;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
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
 * 많이 선택된 취향 키워드 순 프로그램 목록 (비로그인 탐색 탭 배너)
 * - 키워드 순위는 사용자들이 취향 키워드로 고른 수가 많은 순
 * - 프로그램은 가진 키워드 중 가장 높은 순위 그룹에 한 번만, 같은 그룹 안은 최신 등록순
 * - 실제 MySQL 컨테이너에 Flyway 마이그레이션을 적용한 뒤 네이티브 쿼리를 실행
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import(ProgramQueryRepositoryMostSelectedTasteTest.TestAuditConfig.class)
class ProgramQueryRepositoryMostSelectedTasteTest {

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

    // 키워드 순위(선택 수): 1위 3명 / 2위 동점 2명씩 / 3위 1명, 비활성 5명(제외), 아무도 안 고름(제외)
    private static final List<String> EXPECTED_ORDER = List.of(
            "1위 최신",      // 1위 그룹, 가장 최근 등록
            "1위와 3위",     // 1위 그룹 (더 낮은 3위 키워드도 있지만 1위 쪽에 한 번만)
            "1위 오래됨",    // 1위 그룹, 가장 오래 등록
            "2위 동점 최신", // 2위 그룹 (선택 수가 같은 키워드는 같은 그룹)
            "2위와 3위",     // 2위 그룹
            "3위"           // 3위 그룹
    );

    @Autowired
    ProgramQueryRepository programQueryRepository;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void setUp() {
        List<User> users = List.of(user("u1"), user("u2"), user("u3"), user("u4"), user("u5"));

        Keyword first = keyword("1위 키워드");
        Keyword second = keyword("2위 키워드");
        Keyword secondTie = keyword("2위 동점 키워드");
        Keyword third = keyword("3위 키워드");
        Keyword inactive = keyword("비활성 키워드");
        Keyword nobody = keyword("아무도 안 고른 키워드");
        inactive.deactivate();

        select(first, users.get(0), users.get(1), users.get(2));
        select(second, users.get(0), users.get(1));
        select(secondTie, users.get(2), users.get(3));
        select(third, users.get(4));
        select(inactive, users.toArray(User[]::new));

        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 12, 0);
        program("1위 오래됨", base.minusDays(5), first);
        program("1위와 3위", base.minusDays(4), first, third);
        program("1위 최신", base.minusDays(3), first);
        program("2위와 3위", base.minusDays(2), second, third);
        program("2위 동점 최신", base.minusDays(1), secondTie);
        program("3위", base, third);

        // 목록에서 빠져야 하는 프로그램
        program("비활성 키워드만", base, inactive);
        program("아무도 안 고른 키워드만", base, nobody);
        program("키워드 없음", base);
        program("종료된 1위", base, first).updateStatus(ProgramStatus.ENDED);
        program("숨김 1위", base, first).updateStatus(ProgramStatus.HIDDEN);
        program("삭제된 1위", base, first).softDelete(UUID.randomUUID());

        em.flush();
        em.clear();
    }

    @Test
    void 많이_선택된_키워드의_프로그램부터_가장_높은_순위에_한_번만_나온다() {
        Page<Program> page = programQueryRepository.findProgramsOrderByMostSelectedTasteKeyword(PageRequest.of(0, 20));

        assertThat(titles(page)).containsExactlyElementsOf(EXPECTED_ORDER);
        assertThat(page.getTotalElements()).isEqualTo(EXPECTED_ORDER.size());
    }

    @Test
    void 페이지를_넘겨도_같은_순서가_이어지고_전체_개수가_맞다() {
        Page<Program> secondPage = programQueryRepository.findProgramsOrderByMostSelectedTasteKeyword(PageRequest.of(1, 4));

        assertThat(titles(secondPage)).containsExactlyElementsOf(EXPECTED_ORDER.subList(4, 6));
        assertThat(secondPage.getTotalElements()).isEqualTo(EXPECTED_ORDER.size());
    }

    @Test
    void 아무도_취향_키워드를_고르지_않았으면_빈_목록이다() {
        em.getEntityManager().createQuery("DELETE FROM UserKeyword").executeUpdate();
        em.clear();

        Page<Program> page = programQueryRepository.findProgramsOrderByMostSelectedTasteKeyword(PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    private List<String> titles(Page<Program> page) {
        return page.getContent().stream().map(Program::getTitle).toList();
    }

    private User user(String name) {
        return em.persist(User.builder()
                .email(name + "@most-selected-taste.test")
                .password("password")
                .build());
    }

    private Keyword keyword(String name) {
        return em.persist(Keyword.builder()
                .name(name)
                .type(KeywordType.THEME)
                .build());
    }

    private void select(Keyword keyword, User... users) {
        for (User user : users) {
            em.persist(UserKeyword.builder().user(user).keyword(keyword).build());
        }
    }

    // 등록 시각은 감사(Audit)가 현재 시각으로 채우므로, 순서를 고정하려고 저장 후 직접 바꾼다
    private Program program(String title, LocalDateTime createdAt, Keyword... keywords) {
        Program program = em.persist(Program.builder()
                .title(title)
                .programType(ProgramType.EXHIBITION)
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
