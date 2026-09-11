package com.digniche.muntum.curation.service;

import com.digniche.muntum.curation.dto.response.CurationImageResponse;
import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationImage;
import com.digniche.muntum.curation.repository.CurationImageRepository;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurationImageService {

    private static final int MIN_IMAGE_COUNT = 1;
    private static final int MAX_IMAGE_COUNT = 5;
    private static final String DIRECTORY = "curation";

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final CurationImageRepository curationImageRepository;
    private final ImageStorageService imageStorageService;

    /**
     * 신규 큐레이션 이미지 업로드
     */
    @Transactional
    public void uploadImages(Curation curation, List<MultipartFile> files
    ) {
        validateImageFiles(files);

        List<CurationImage> images = buildImages(curation, files);

        curationImageRepository.saveAll(images);
    }

    /**
     * 큐레이션 이미지 전체 교체
     */
    @Transactional
    public void replaceImages(Curation curation, List<MultipartFile> files
    ) {
        /*
         * 기존 이미지 삭제 전 새로운 이미지 먼저 검증
         */
        validateImageFiles(files);

        /*
         * 기존 이미지 유지한 상태에서 새로운 이미지 스토리지에 업로드
         */
        List<CurationImage> newImages = buildImages(curation, files);

        List<CurationImage> existingImages = curationImageRepository.findByCuration_IdOrderByDisplayOrderAsc(curation.getId());

        /*
         * 같은 displayOrder의 유니크제약충돌 막기 위해 기존 DB행 먼저 삭제후 flush
         */
        curationImageRepository.deleteAllByCurationId(curation.getId());
        curationImageRepository.flush();

        curationImageRepository.saveAll(newImages);

        /*
         * DB 교체 작업 이후 기존 스토리지 파일 삭제(스토리지 삭제 실패는 DB 작업을 실패시키지 않음)
         */
        existingImages.forEach(this::deleteStoredImageSafely
        );
    }

    /**
     * 큐레이션 이미지 순서대로 조회
     */
    public List<CurationImageResponse> getOrderedImages(
            UUID curationId
    ) {
        return curationImageRepository.findByCuration_IdOrderByDisplayOrderAsc(curationId
                )
                .stream()
                .map(CurationImageResponse::from)
                .toList();
    }

    /**
     * 큐레이션 이미지 전체 삭제
     */
    @Transactional
    public void deleteImages(UUID curationId) {
        List<CurationImage> existingImages = curationImageRepository.findByCuration_IdOrderByDisplayOrderAsc(curationId);

        curationImageRepository.deleteAllByCurationId(curationId);

        existingImages.forEach(this::deleteStoredImageSafely);
    }

    /**
     * 이미지 개수 및 파일 형식 검증
     */
    private void validateImageFiles(
            List<MultipartFile> files
    ) {
        if (files == null
                || files.size() < MIN_IMAGE_COUNT) {
            throw new BusinessException(
                    ErrorCode.CURATION_IMAGE_REQUIRED
            );
        }

        if (files.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(
                    ErrorCode.TOO_MANY_CURATION_IMAGES
            );
        }

        files.forEach(this::validateImageFile);
    }

    /**
     * 개별 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null
                || file.isEmpty()
                || !ALLOWED_CONTENT_TYPES.contains(
                file.getContentType()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_IMAGE_FILE
            );
        }
    }

    /**
     * 이미지 업로드 후 엔티티 목록 생성
     */
    private List<CurationImage> buildImages(Curation curation, List<MultipartFile> files
    ) {
        List<CurationImage> images = new ArrayList<>();

        for (int index = 0;
             index < files.size();
             index++) {

            MultipartFile file = files.get(index);

            String imageUrl = imageStorageService.upload(file, DIRECTORY);

            CurationImage image = CurationImage.builder().curation(curation).imageUrl(imageUrl).displayOrder(index + 1).build();
            images.add(image);
        }

        return images;
    }

    /**
     * 스토리지 삭제 실패가 DB 작업 막지 않도록 처리
     */
    private void deleteStoredImageSafely(CurationImage image) {
        try {
            imageStorageService.delete(image.getImageUrl());
        } catch (Exception exception) {
            log.warn(
                    "큐레이션 이미지 파일 삭제 실패, 수동 정리 필요: {}",
                    image.getImageUrl(),
                    exception
            );
        }
    }

    /**
     * 대표 이미지 일괄 조회
     */
    public Map<UUID, String> getThumbnailMap(
            Collection<UUID> curationIds
    ) {
        if (curationIds == null
                || curationIds.isEmpty()) {
            return Map.of();
        }

        return curationImageRepository
                .findThumbnailsByCurationIds(curationIds)
                .stream()
                .collect(Collectors.toMap(
                        image -> image.getCuration().getId(),
                        CurationImage::getImageUrl
                ));
    }
}