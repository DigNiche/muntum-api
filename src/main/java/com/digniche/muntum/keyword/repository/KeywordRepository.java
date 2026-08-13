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
     * - 생성자, 수정자, 삭제자 Withdrawn UUID로 채우기
     */

    @Modifying
    @Query("UPDATE Keyword k SET k.createdBy = :withdrawnUuid WHERE k.createdBy = :userId")
    void replaceCreatedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    @Modifying
    @Query("UPDATE Keyword k SET k.updatedBy = :withdrawnUuid WHERE k.updatedBy = :userId")
    void replaceUpdatedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);

    @Modifying
    @Query("UPDATE Keyword k SET k.deletedBy = :withdrawnUuid WHERE k.deletedBy = :userId")
    void replaceDeletedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);
}
