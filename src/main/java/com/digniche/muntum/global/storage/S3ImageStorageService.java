package com.digniche.muntum.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;
/**
 * 배포/운영 환경에서 파일 업로드 및 삭제하는 서비스
 */
@Component
@Profile("prod")
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String cloudfrontDomain;

    public S3ImageStorageService(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.cloudfront.domain}") String cloudfrontDomain
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.cloudfrontDomain = cloudfrontDomain;
    }
    @Override
    public String upload(MultipartFile file, String directory) {
        String key = directory + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new RuntimeException("S3 이미지 업로드 실패: " + key, e);
        }
        return cloudfrontDomain + "/" + key;
    }

    @Override
    public void delete(String imageUrl) {
        String key = imageUrl.replace(cloudfrontDomain + "/", "");
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }
}
