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

    private static final int MAX_IMAGE_COUNT = 5;
    private static final String DIRECTORY = "curation";

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final CurationImageRepository curationImageRepository;
    private final ImageStorageService imageStorageService;

    /**
     * 신규 큐레이션 이미지 업로드 (이미지는 선택 입력, 0~5장)
     * - 저장한 이미지를 순서대로 반환 (재조회 없음)
     * - @Transactional: 클래스 기본값(readOnly)을 쓰기로 덮어씀, 호출한 쪽 트랜잭션이 있으면 거기에 합류
     */
    @Transactional
    public List<CurationImageResponse> uploadImages(Curation curation, List<MultipartFile> files) {

        // 업로드된 이미지가 없는 경우
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        // 이미지 파일 유효성 검증
        validateImageFiles(files);

        // 저장소에 이미지 업로드 후, 엔티티 생성
        List<CurationImage> images = buildImages(curation, files);

        // DB에 엔티티 저장
        return toResponses(curationImageRepository.saveAll(images));
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
        existingImages.forEach(this::deleteStoredImageSafely);
    }

    /**
     * 큐레이션 이미지 순서대로 조회
     */
    public List<CurationImageResponse> getOrderedImages(UUID curationId) {
        return curationImageRepository.findByCuration_IdOrderByDisplayOrderAsc(curationId)
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
    private void validateImageFiles(List<MultipartFile> files) {
        // 업로드 가능한 파일 개수를 초과한 경우
        if (files.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.TOO_MANY_CURATION_IMAGES);
        }
        // 개별 이미지 파일 검증
        files.forEach(this::validateImageFile);
    }

    /**
     * 개별 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        // 객체가 없는 경우 || 객체가 있지만 0 byte 혹은 깨진 리소스인 경우 || 지원하지 않는 파일 형식인 경우
        if (file == null || file.isEmpty() || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    /**
     * 외부/로컬 이미지 저장소에 업로드 후 엔티티 생성
     */
    private List<CurationImage> buildImages(Curation curation, List<MultipartFile> files) {
        List<CurationImage> images = new ArrayList<>();

        for (int index = 0; index < files.size(); index++) {

            MultipartFile file = files.get(index);
            String imageUrl = imageStorageService.upload(file, DIRECTORY);

            CurationImage image = CurationImage.builder().curation(curation).imageUrl(imageUrl).displayOrder(index + 1).build();
            // display order 순서대로 저장됨
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


    /**
     * Entity -> DTO 변환
     */
    private List<CurationImageResponse> toResponses(List<CurationImage> images) {
        return images.stream()
                .sorted(Comparator.comparingInt(CurationImage::getDisplayOrder)) // 명시적 display order 기준 정렬
                .map(CurationImageResponse::from).toList();
    }
}