package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.ProgramImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * 프로그램 이미지 Repository
 */
public interface ProgramImageRepository extends JpaRepository<ProgramImage, UUID> {

    List<ProgramImage> findByProgramIdOrderByDisplayOrderAsc(UUID programId);

    @Query("SELECT pi FROM ProgramImage pi JOIN pi.program p WHERE pi.displayOrder = 1 AND p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    List<ProgramImage> findThumbnailsOfActivePrograms();

    void deleteByProgramId(UUID programId);
}
