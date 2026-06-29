package com.digniche.muntum.keyword.repository;

import com.digniche.muntum.keyword.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
