package com.digniche.muntum.program.repository;

import com.digniche.muntum.program.entity.ProgramImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramImageRepository extends JpaRepository<ProgramImage, UUID> {


    // 상세 조회용: 이 프로그램의 이미지를 순서대로 조회
    List<ProgramImage> findByProgramIdOrderByDisplayOrderAsc(UUID programId);
    // 이 프로그램의 모든 이미지 삭제 (PUT 교체용)
    void deleteByProgramId(UUID programId);

}