package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.storage.ImageStorageService;
import com.digniche.muntum.program.dto.response.ProgramImageResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 프로그램 이미지 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramImageService {

    private final ProgramImageRepository programImageRepository;
    private final ImageStorageService imageStorageService;

    private static final int THUMBNAIL_ORDER = 1;
    private static final int MAX_IMAGE_COUNT = 5;
    private static final String DIRECTORY = "program";
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    // 이미지 업로드
    @Transactional
    public void uploadImages(Program program, List<MultipartFile> files) {
        // 이미지 파일 검증
        if (files.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.TOO_MANY_PROGRAM_IMAGES);
        }
        files.forEach(this::validateImageFile);

        // 스토리지에 이미지 업로드
        List<ProgramImage> images = buildImages(program, files);

        // DB에 이미지 URL 저장
        programImageRepository.saveAll(images);
    }

    // 이미지 수정
    @Transactional
    public void replaceImages(Program program, List<MultipartFile> files) {
        // 이미지 검증
        if (files.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.TOO_MANY_PROGRAM_IMAGES);
        }
        files.forEach(this::validateImageFile);

        // 새로운 이미지 스토리지에 업로드
        List<ProgramImage> newImages = buildImages(program, files);

        // 기존 이미지 조회
        List<ProgramImage> existingImages =
                programImageRepository.findByProgramIdOrderByDisplayOrderAsc(program.getId());

        // DB에서 기존 이미지 삭제
        programImageRepository.deleteAllByProgramId(program.getId());

        // 스토리지에서 기존 이미지 삭제
        existingImages.forEach(this::deleteStoredImageSafely);

        // 새로운 이미지 DB에 저장
        programImageRepository.saveAll(newImages);
    }

    // 프로그램 별 이미지 목록 조회
    @Transactional(readOnly = true)
    public List<ProgramImageResponse> getOrderedImages(UUID programId) {
        return programImageRepository.findByProgramIdOrderByDisplayOrderAsc(programId)
                .stream()
                .map(ProgramImageResponse::from)
                .toList();
    }

    // 썸네일(display order = 1) 이미지 목록 조회
    @Transactional(readOnly = true)
    public List<ProgramImageResponse> getThumbnails() {
        return programImageRepository.findThumbnailsOfActivePrograms()
                .stream().map(ProgramImageResponse::from).toList();
    }

    // 썸네일 이미지 단건 조회
    @Transactional(readOnly = true)
    public String getThumbnail(UUID programId) {
        ProgramImage img = programImageRepository.findByProgramIdAndDisplayOrder(programId, THUMBNAIL_ORDER).orElseThrow(() -> new BusinessException(ErrorCode.PROGRAM_IMAGE_NOT_FOUND));
        return img.getImageUrl();
    }


    /**
     * 여러 프로그램의 썸네일(displayOrder=1) Map 생성 - N+1 방지용 IN 조회
     * key = programId, value = 썸네일 URL
     */
    @Transactional(readOnly = true)
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
                    )
                );
    }

    // 이미지 파일 검증
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    // 스토리지에서 이미지 삭제 : 기존 파일 정리에 실패해도 수정 트랜잭션 자체를 막지 않고 로그만 남기도록
    private void deleteStoredImageSafely(ProgramImage image) {
        try {
            imageStorageService.delete(image.getImageUrl());
        } catch (Exception e) {
            log.warn("기존 이미지 파일 삭제 실패, 수동 정리 필요: {}", image.getImageUrl(), e);
        }
    }

    // 스토리지에 이미지 업로드
    private List<ProgramImage> buildImages(Program program, List<MultipartFile> files) {
        List<ProgramImage> images = new java.util.ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String imageUrl = imageStorageService.upload(file, DIRECTORY);
            images.add(ProgramImage.builder()
                    .program(program)
                    .imageUrl(imageUrl)
                    .displayOrder(i + 1)
                    .build());
        }
        return images;
    }
}
