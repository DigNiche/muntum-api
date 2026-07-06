package com.digniche.muntum.keyword.repository;

import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.program.entity.ProgramKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Program - keyword Repository
 */
public interface ProgramKeywordRepository extends JpaRepository<ProgramKeyword, UUID> {
    // Keyword ID로 모두 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProgramKeyword pk WHERE pk.keyword.id = :keywordId")
    void deleteAllByKeywordId(@Param("keywordId") UUID keywordId);

    // Program ID로 모두 삭제
    @Modifying
    @Query("DELETE FROM ProgramKeyword pk WHERE pk.program.id = :programId")
    void deleteAllByProgramId(@Param("programId") UUID programId);

    // Program ID로 모두 조회 (상세에서 키워드 태그 노출용)
    @Query("""
    SELECT pk
    FROM ProgramKeyword pk
    JOIN FETCH pk.keyword
    WHERE pk.program.id = :programId
""")
    List<ProgramKeyword> findByProgramId(@Param("programId") UUID programId);

    @Query("""
    SELECT pk
    FROM ProgramKeyword pk
    JOIN FETCH pk.keyword
    WHERE pk.program.id IN :programIds
""")
    List<ProgramKeyword> findByProgramIdIn(@Param("programIds") Collection<UUID> programIds);

    // Program과 연결된 키워드 목록 조회
    @Query("SELECT DISTINCT pk.keyword FROM ProgramKeyword pk")
    List<Keyword> findAllKeywords();


}

