package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.storage.ImageStorageService;
import com.digniche.muntum.program.dto.response.ProgramImageResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramImageService {

    private final ProgramImageRepository programImageRepository;
    private final ProgramRepository programRepository;
    private final ImageStorageService imageStorageService;

    private static final int THUMBNAIL_ORDER = 1;
    private static final String DIRECTORY = "program";
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    // 이미지 업로드
    @Transactional
    public void uploadImages(Program program, List<MultipartFile> files) {
        List<ProgramImage> images = new java.util.ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            validateImageFile(file);

            String imageUrl = imageStorageService.upload(file, DIRECTORY);

            images.add(ProgramImage.builder()
                    .program(program)
                    .imageUrl(imageUrl)
                    .displayOrder(i + 1)
                    .build());
        }
        programImageRepository.saveAll(images);
    }

    // 프로그램 별 이미지 목록 조회
    public List<ProgramImageResponse> getOrderedImages(UUID programId) {
        return programImageRepository.findByProgramIdOrderByDisplayOrderAsc(programId)
                .stream()
                .map(ProgramImageResponse::from)
                .toList();
    }

    // 썸네일(display order = 1) 이미지 목록 조회
    public List<ProgramImageResponse> getThumbnails() {
        return programImageRepository.findThumbnailsOfActivePrograms()
                .stream().map(ProgramImageResponse::from).toList();
    }

    public String getThumbnail(UUID programId) {
        ProgramImage img = programImageRepository.findByProgramIdAndDisplayOrder(programId, THUMBNAIL_ORDER).orElseThrow(() -> new BusinessException(ErrorCode.PROGRAM_IMAGE_NOT_FOUND));
        return img.getImageUrl();
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
                    )
                );
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }
}
