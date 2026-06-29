package com.digniche.muntum.keyword.repository;


import com.digniche.muntum.keyword.entity.UserKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * 사용자-키워드 연결 Repository
 */
public interface UserKeywordRepository extends JpaRepository<UserKeyword, UUID> {
    @Modifying
    @Query("DELETE FROM UserKeyword uk WHERE uk.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserKeyword uk WHERE uk.keyword.id = :keywordId")
    void deleteAllByKeywordId(@Param("keywordId") UUID keywordId);
}