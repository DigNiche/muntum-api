package com.digniche.muntum.keyword.repository;

import com.digniche.muntum.keyword.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 키워드 JPA Repository
 */
public interface KeywordRepository extends JpaRepository<Keyword, UUID> {
    List<Keyword> findAllByNameInAndActiveTrue(List<String> names);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID keywordId);
    List<Keyword> findAllByIdInAndActiveTrue(List<UUID> ids);

    // 생성자: System UUID로 채우기
    @Modifying
    @Query(value = "UPDATE keywords SET created_by = :systemUuid WHERE created_by = :userId", nativeQuery = true)
    void replaceCreatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    // 수정자: Null 처리
    @Modifying
    @Query(value = "UPDATE keywords SET updated_by = NULL WHERE updated_by = :userId", nativeQuery = true)
    void nullifyUpdatedBy(@Param("userId") UUID userId);

    // 삭제자: Null 처리
    @Modifying
    @Query("UPDATE Keyword k SET k.deletedBy = null WHERE k.deletedBy = :userId")
    void nullifyDeletedBy(@Param("userId") UUID userId);
}
