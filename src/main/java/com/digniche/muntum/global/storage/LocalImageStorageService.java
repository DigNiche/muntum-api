package com.digniche.muntum.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬 환경에서 파일 업로드 및 삭제하는 서비스
 */
@Component
@Profile("local")
public class LocalImageStorageService implements ImageStorageService{

    private final String basePath;
    private final String baseUrl;

    public LocalImageStorageService(@Value("${storage.local.base-path}") String basePath, @Value("${storage.local.base-url}") String baseUrl) {
        this.basePath = basePath;
        this.baseUrl = baseUrl;
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dirPath = Paths.get(basePath, directory);
        Path filePath = dirPath.resolve(filename);  // dirPath/directory/filename

        try {
            Files.createDirectories(dirPath);
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("로컬 이미지 저장 실패: " + filePath, e);
        }
        return baseUrl + "/" + directory + "/" + filename;
    }


    @Override
    public void delete(String imageUrl) {
        String relativePath = imageUrl.replace(baseUrl + "/", "");
        Path filePath = Paths.get(basePath, relativePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("로컬 이미지 삭제 실패: " + filePath, e);
        }
    }
}
