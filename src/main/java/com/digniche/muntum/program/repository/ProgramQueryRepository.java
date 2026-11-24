package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.Program;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.UUID;

/**
 * 프로그램 조회(Query) Repository
 */
public interface ProgramQueryRepository extends Repository<Program, UUID> {

    /**
     * 많이 선택된 취향 키워드 순 프로그램 목록 조회
     *
     * [ 요약 ]
     * - 키워드 순위 : 사용자들이 취향 키워드로 고른 수(user_keywords)가 많은 순. 활성 키워드만.
     *             : 선택 수가 같은 키워드는 같은 순위 그룹에 속한다
     * - 프로그램은 가진 키워드 중 가장 많이 선택된 키워드의 순위 그룹에 한 번만 나온다
     * - 같은 그룹 안에서는 최신 등록순
     * - 성능: 요청마다 user_keywords 전체를 키워드별로 집계한다. 주기적으로 미리 집계해 두고 조회하는 방식으로 바꿀 예정이다.
     *
     * [ 상세 ]
     * - 1. 키워드 순위 쿼리(kr)
     *   selected_count = user_keywords에서 키워드 별로 count한 값. 활성(is_active) 키워드만 센다
     *   순위 번호는 따로 매기지 않고, selected_count를 순위 값으로 사용하기 때문에 고른 사용자 수가 같은 키워드는 같은 순위가 된다
     * - 2. 프로그램 순위 쿼리(ranked)
     *   프로그램은 여러 키워드를 가지기 때문에 하나의 프로그램에 여러 키워드 점수(selected_count들)를 가진다.
     *   프로그램 점수는 그 키워드 점수들 중 가장 큰 값(best_selected_count)으로 정해진다. 이것을 프로그램의 순위값으로 사용한다.
     *   프로그램 당 한 행만 남기 때문에 순위 키워드를 여러 개 가져도 가장 높은 순위에 한 번만 나온다.
     * - 3. 조건과 정렬
     *   조회 프로그램 대상 : 운영 중(ACTIVE)인 + 삭제되지 않은 프로그램들
     *   best_selected_count DESC > created_at DESC > id(등록 시각 같을 경우 페이지 사이 순서가 흔들리지 않도록) DESC
     * - 4. count 쿼리
     *   위와 같은 조건으로, 순위 키워드를 하나라도 가진 운영 중 프로그램 수를 센다.
     */
    @Query(
            value = """
                    SELECT p.*
                    FROM programs p
                    JOIN (
                        SELECT pk.program_id, MAX(kr.selected_count) AS best_selected_count
                        FROM program_keywords pk
                        JOIN (
                            SELECT uk.keyword_id, COUNT(*) AS selected_count
                            FROM user_keywords uk
                            JOIN keywords k ON k.id = uk.keyword_id
                            WHERE k.is_active = TRUE
                            GROUP BY uk.keyword_id
                        ) kr ON kr.keyword_id = pk.keyword_id
                        GROUP BY pk.program_id
                    ) ranked ON ranked.program_id = p.id
                    WHERE p.status = 'ACTIVE'
                      AND p.deleted_at IS NULL
                    ORDER BY ranked.best_selected_count DESC,
                             p.created_at DESC,
                             p.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT p.id)
                    FROM programs p
                    JOIN program_keywords pk ON pk.program_id = p.id
                    JOIN user_keywords uk ON uk.keyword_id = pk.keyword_id
                    JOIN keywords k ON k.id = uk.keyword_id
                    WHERE p.status = 'ACTIVE'
                      AND p.deleted_at IS NULL
                      AND k.is_active = TRUE
                    """,
            nativeQuery = true
    )
    Page<Program> findProgramsOrderByMostSelectedTasteKeyword(Pageable pageable);
}
