package com.digniche.muntum.program.repository;

import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.entity.KeywordType;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramKeyword;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.entity.ReservationType;
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
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * '예약없이' 필터는 예약 방식(reservationType)으로 거른다
 * - 포함: 현장예매, 사전예약·현장예매, 자유관람 / 제외: 사전예약 전용, 예약 방식 없음(null)
 * - 실제 MySQL 컨테이너에 Flyway 마이그레이션을 적용한 뒤 저장소 쿼리를 실행
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import(ProgramRepositoryNoReservationFilterTest.TestAuditConfig.class)
class ProgramRepositoryNoReservationFilterTest {

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

    private static final List<ReservationType> NO_RESERVATION_TYPES = ReservationType.NO_RESERVATION_TYPES;
    private static final List<ProgramStatus> STATUSES = List.of(ProgramStatus.ACTIVE);
    private static final String SEARCH_WORD = "예약필터";
    private static final Pageable PAGE = PageRequest.of(0, 20);

    // 칩을 켰을 때 나와야 하는 프로그램
    private static final List<String> NO_RESERVATION_TITLES = List.of(
            "예약필터 현장예매", "예약필터 사전예약·현장예매", "예약필터 자유관람");
    // 칩을 껐을 때 나와야 하는 프로그램 전체
    private static final List<String> ALL_TITLES = List.of(
            "예약필터 현장예매", "예약필터 사전예약·현장예매", "예약필터 자유관람",
            "예약필터 사전예약", "예약필터 예약 방식 없음");

    @Autowired
    ProgramRepository programRepository;

    @Autowired
    TestEntityManager em;

    private List<UUID> keywordIds;

    @BeforeEach
    void setUp() {
        Keyword keyword = em.persist(Keyword.builder()
                .name("예약필터 키워드")
                .type(KeywordType.THEME)
                .build());
        keywordIds = List.of(keyword.getId());

        save("예약필터 현장예매", ReservationType.ON_SITE, keyword);
        save("예약필터 사전예약·현장예매", ReservationType.PRE_REGISTRATION_AND_ON_SITE, keyword);
        save("예약필터 자유관람", ReservationType.FREE_ENTRY, keyword);
        save("예약필터 사전예약", ReservationType.PRE_REGISTRATION, keyword);
        save("예약필터 예약 방식 없음", null, keyword);

        em.flush();
        em.clear();
    }

    @Test
    void 일반_목록은_예약없이_칩을_켜면_예약_방식으로만_거른다() {
        Page<Program> on = programRepository.findProgramsWithFilter(
                STATUSES, null, true, NO_RESERVATION_TYPES, null, null, null, PAGE);
        Page<Program> off = programRepository.findProgramsWithFilter(
                STATUSES, null, null, NO_RESERVATION_TYPES, null, null, null, PAGE);

        assertFiltered(on, off);
    }

    @Test
    void 인기_키워드가_없을_때_대체_목록은_예약없이_칩을_켜면_예약_방식으로만_거른다() {
        Page<Program> on = programRepository.findFilteredProgramsOrderByLatest(
                STATUSES, LocalDate.now(), null, true, NO_RESERVATION_TYPES, null, null, null, PAGE);
        Page<Program> off = programRepository.findFilteredProgramsOrderByLatest(
                STATUSES, LocalDate.now(), null, null, NO_RESERVATION_TYPES, null, null, null, PAGE);

        assertFiltered(on, off);
    }

    @Test
    void 인기_키워드_목록은_예약없이_칩을_켜면_예약_방식으로만_거른다() {
        Page<Program> on = programRepository.findProgramsByKeywordIds(
                STATUSES, keywordIds, LocalDate.now(), null, true, NO_RESERVATION_TYPES, null, null, null, PAGE);
        Page<Program> off = programRepository.findProgramsByKeywordIds(
                STATUSES, keywordIds, LocalDate.now(), null, null, NO_RESERVATION_TYPES, null, null, null, PAGE);

        assertFiltered(on, off);
    }

    @Test
    void 키워드_검색은_예약없이_칩을_켜면_예약_방식으로만_거른다() {
        Page<Program> on = programRepository.searchProgramsByKeywordIds(
                STATUSES, keywordIds, LocalDate.now(), null, true, NO_RESERVATION_TYPES, null, null, null, PAGE);
        Page<Program> off = programRepository.searchProgramsByKeywordIds(
                STATUSES, keywordIds, LocalDate.now(), null, null, NO_RESERVATION_TYPES, null, null, null, PAGE);

        assertFiltered(on, off);
    }

    @Test
    void 텍스트_검색은_예약없이_칩을_켜면_예약_방식으로만_거른다() {
        String pattern = "%" + SEARCH_WORD + "%";

        Page<Program> on = programRepository.searchProgramsByText(
                STATUSES, pattern, LocalDate.now(), null, true, NO_RESERVATION_TYPES, null, null, null,
                CurationPublicationStatus.PUBLISHED, PAGE);
        Page<Program> off = programRepository.searchProgramsByText(
                STATUSES, pattern, LocalDate.now(), null, null, NO_RESERVATION_TYPES, null, null, null,
                CurationPublicationStatus.PUBLISHED, PAGE);

        assertFiltered(on, off);
    }

    @Test
    void 내_취향_목록은_예약없이_칩을_켜면_예약_방식으로만_거른다() {
        Page<Program> on = programRepository.searchTasteProgramsByKeywordIds(
                STATUSES, keywordIds, null, true, NO_RESERVATION_TYPES, null, null, null, PAGE);
        Page<Program> off = programRepository.searchTasteProgramsByKeywordIds(
                STATUSES, keywordIds, null, null, NO_RESERVATION_TYPES, null, null, null, PAGE);

        assertFiltered(on, off);
    }

    @Test
    void 지도_목록은_예약없이_칩을_켜면_예약_방식으로만_거른다() {
        List<Program> on = programRepository.findProgramsInBounds(
                STATUSES, 37.0, 126.0, 38.0, 128.0,
                null, true, NO_RESERVATION_TYPES, null, null, null, Limit.of(200));
        List<Program> off = programRepository.findProgramsInBounds(
                STATUSES, 37.0, 126.0, 38.0, 128.0,
                null, null, NO_RESERVATION_TYPES, null, null, null, Limit.of(200));

        assertThat(titles(on)).containsExactlyInAnyOrderElementsOf(NO_RESERVATION_TITLES);
        assertThat(titles(off)).containsExactlyInAnyOrderElementsOf(ALL_TITLES);
    }

    // 칩을 켜면 예약 방식 3종만, 끄면 전체가 나오고 전체 개수(count 쿼리)도 결과와 같아야 함
    private void assertFiltered(Page<Program> on, Page<Program> off) {
        assertThat(titles(on.getContent())).containsExactlyInAnyOrderElementsOf(NO_RESERVATION_TITLES);
        assertThat(on.getTotalElements()).isEqualTo(NO_RESERVATION_TITLES.size());

        assertThat(titles(off.getContent())).containsExactlyInAnyOrderElementsOf(ALL_TITLES);
        assertThat(off.getTotalElements()).isEqualTo(ALL_TITLES.size());
    }

    private List<String> titles(List<Program> programs) {
        return programs.stream().map(Program::getTitle).toList();
    }

    private void save(String title, ReservationType reservationType, Keyword keyword) {
        Program program = em.persist(Program.builder()
                .title(title)
                .programType(ProgramType.EXHIBITION)
                .description(title + " 소개")
                .reservationType(reservationType)
                .free(true)
                .venueName("테스트 장소")
                .address("서울시 테스트구")
                .latitude(new BigDecimal("37.5"))
                .longitude(new BigDecimal("127.0"))
                .build());
        em.persist(ProgramKeyword.builder()
                .program(program)
                .keyword(keyword)
                .build());
    }
}
