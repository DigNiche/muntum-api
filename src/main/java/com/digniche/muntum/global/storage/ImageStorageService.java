package com.digniche.muntum.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 저장소 인터페이스
 * - 개발 / Prod 환경 구현체 필요
 */
public interface ImageStorageService {
    String upload(MultipartFile file, String directory);
    void delete(String imageUrl);
}
