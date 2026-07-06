package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.ProgramImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 프로그램 이미지 Repository
 */
public interface ProgramImageRepository extends JpaRepository<ProgramImage, UUID> {

    List<ProgramImage> findByProgramIdOrderByDisplayOrderAsc(UUID programId);

    // 목록 썸네일용: 여러 프로그램의 1번 이미지(썸네일)를 한 번에 조회
    List<ProgramImage> findByProgramIdInAndDisplayOrder(Collection<UUID> programIds, int displayOrder);

    // Program Id로 조회하는 썸네일 이미지
    Optional<ProgramImage> findByProgramIdAndDisplayOrder(UUID programId, int displayOrder);

    @Query("SELECT pi FROM ProgramImage pi JOIN pi.program p WHERE pi.displayOrder = 1 AND p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    List<ProgramImage> findThumbnailsOfActivePrograms();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProgramImage pi WHERE pi.program.id = :programId")
    void deleteAllByProgramId(@Param("programId") UUID programId);

    void deleteByProgramId(UUID programId);
}
