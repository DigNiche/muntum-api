package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 프로그램 데이터 접근 계층
 */
public interface ProgramRepository extends JpaRepository<Program, UUID> {

    // 단건 조회 - 삭제되지 않은(deletedAt이 null인) 프로그램만
    Optional<Program> findByIdAndDeletedAtIsNull(UUID id);

    // 목록 조회 - 삭제되지 않은 프로그램만, 페이징 적용
    Page<Program> findByDeletedAtIsNull(Pageable pageable);
}