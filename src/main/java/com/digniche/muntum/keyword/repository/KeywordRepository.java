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

    /**
     * 사용자 삭제 시
     * - 생성자, 수정자, 삭제자 System UUID로 채우기
     */

    @Modifying
    @Query("UPDATE Keyword k SET k.createdBy = :systemUuid WHERE k.createdBy = :userId")
    void replaceCreatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Keyword k SET k.updatedBy = :systemUuid WHERE k.updatedBy = :userId")
    void replaceUpdatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Keyword k SET k.deletedBy = :systemUuid WHERE k.deletedBy = :userId")
    void replaceDeletedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);
}
