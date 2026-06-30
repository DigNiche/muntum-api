package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.ProgramImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 프로그램 이미지 Repository
 */
public interface ProgramImageRepository extends JpaRepository<ProgramImage, UUID> {

    List<ProgramImage> findByProgramIdOrderByDisplayOrderAsc(UUID programId);

    void deleteByProgramId(UUID programId);
}
