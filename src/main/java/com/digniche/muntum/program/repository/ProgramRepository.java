package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.entity.ProgramStatus;
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

    // 단건
    Optional<Program> findByIdAndDeletedAtIsNullAndStatus(UUID id, ProgramStatus status);

    // 목록
    Page<Program> findByDeletedAtIsNullAndStatus(ProgramStatus status, Pageable pageable);
}