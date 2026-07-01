package com.digniche.muntum.keyword.repository;

import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.program.entity.ProgramKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Progrma - keyword Repository
 */
public interface ProgramKeywordRepository extends JpaRepository<ProgramKeyword, UUID> {
    @Modifying
    @Query("DELETE FROM ProgramKeyword pk WHERE pk.keyword.id = :keywordId")
    void deleteAllByKeywordId(@Param("keywordId") UUID keywordId);

    //이 프로그램의 연결 전부 삭제 (PUT 교체용)
    @Modifying
    @Query("DELETE FROM ProgramKeyword pk WHERE pk.program.id = :programId")
    void deleteByProgramId(@Param("programId") UUID programId);

    //이 프로그램에 붙은 연결 조회 (상세에서 키워드 태그 노출용)
    @Query("""
    SELECT pk
    FROM ProgramKeyword pk
    JOIN FETCH pk.keyword
    WHERE pk.program.id = :programId
""")
    List<ProgramKeyword> findByProgramId(@Param("programId") UUID programId);

}

