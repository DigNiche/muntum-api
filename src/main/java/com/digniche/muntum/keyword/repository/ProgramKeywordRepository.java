package com.digniche.muntum.keyword.repository;

import com.digniche.muntum.program.entity.ProgramKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Progrma - keyword Repository
 */
public interface ProgramKeywordRepository extends JpaRepository<ProgramKeyword, UUID> {
    @Modifying
    @Query("DELETE FROM ProgramKeyword pk WHERE pk.keyword.id = :keywordId")
    void deleteAllByKeywordId(@Param("keywordId") UUID keywordId);
}
