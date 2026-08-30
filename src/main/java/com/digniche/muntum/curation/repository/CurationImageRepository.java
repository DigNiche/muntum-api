package com.digniche.muntum.curation.repository;

import com.digniche.muntum.curation.entity.CurationImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurationImageRepository
        extends JpaRepository<CurationImage, UUID> {

    /**
     * 큐레이션 이미지 순서대로 조회
     */
    List<CurationImage> findByCuration_IdOrderByDisplayOrderAsc(
            UUID curationId
    );

    /**
     * 특정 큐레이션의 특정 이미지 조회
     *
     * 이미지 ID와 curationId를 함께 확인해서
     * 다른 큐레이션의 이미지를 수정·삭제하지 못하게 함.
     */
    Optional<CurationImage> findByIdAndCuration_Id(
            UUID imageId,
            UUID curationId
    );


    /**
     * 이미지 개수 확인
     */
    long countByCuration_Id(
            UUID curationId
    );

    /**
     * 큐레이션 이미지 전체 삭제
     *
     * 수정 후 재업로드 or 큐레이션 정리에 사용
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM ProgramCurationImage image
        WHERE image.programCuration.id = :curationId
    """)
    int deleteAllByCurationId(
            @Param("curationId") UUID curationId
    );
}