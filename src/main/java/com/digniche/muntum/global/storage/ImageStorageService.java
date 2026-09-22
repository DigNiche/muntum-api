package com.digniche.muntum.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 저장소 인터페이스
 * - 개발 / Prod 환경 구현체 필요
 */
public interface ImageStorageService {
    String upload(MultipartFile file, String directory);
    void delete(String imageUrl);

    /* 반드시 삭제해야 할 이미지 파일이 있을 경우
    default void deleteQuietly(String imageUrl) {
        try {
            delete(imageUrl);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ImageStorageService.class).warn("이미지 파일 삭제 실패. 수동 정리 필요: {}", imageUrl, e);
        }
    }
    */
}
