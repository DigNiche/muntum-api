package com.digniche.muntum.program.service;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 프로그램 이미지 저장 / 조회 / 썸네일 Map 생성 담당
 * - 여러 서비스(Program, Scrap 등)가 공통으로 사용하는 이미지 전용 컴포넌트
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramImageService {

    private static final int THUMBNAIL_ORDER = 1;

    private final ProgramImageRepository programImageRepository;

    /**
     * 이미지 저장 (displayOrder 1부터 자동 부여, 1번 = 썸네일)
     */
    @Transactional
    public void saveImages(Program program, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (int i = 0; i < imageUrls.size(); i++) {
            ProgramImage image = ProgramImage.builder()
                    .program(program)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i + 1)
                    .build();

            programImageRepository.save(image);
        }
    }

    /**
     * 이미지 전체 교체 (PUT용): 기존 삭제 → flush → 새로 저장
     */
    @Transactional
    public void replaceImages(Program program, List<String> imageUrls) {
        programImageRepository.deleteByProgramId(program.getId());
        programImageRepository.flush();
        saveImages(program, imageUrls);
    }

    /**
     * 한 프로그램의 이미지 URL 목록 (displayOrder 순)
     */
    public List<String> getImageUrls(UUID programId) {
        return programImageRepository.findByProgramIdOrderByDisplayOrderAsc(programId)
                .stream()
                .map(ProgramImage::getImageUrl)
                .toList();
    }

    /**
     * 여러 프로그램의 썸네일(displayOrder=1) Map 생성 - N+1 방지용 IN 조회
     * key = programId, value = 썸네일 URL
     */
    public Map<UUID, String> getThumbnailMap(Collection<UUID> programIds) {
        if (programIds == null || programIds.isEmpty()) {
            return Map.of();
        }
        return programImageRepository
                .findByProgramIdInAndDisplayOrder(programIds, THUMBNAIL_ORDER)
                .stream()
                .collect(Collectors.toMap(
                        img -> img.getProgram().getId(),
                        ProgramImage::getImageUrl,
                        (existing, replacement) -> existing
                ));
    }
}