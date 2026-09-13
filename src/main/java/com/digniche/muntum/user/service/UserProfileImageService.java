package com.digniche.muntum.user.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.storage.ImageStorageService;
import com.digniche.muntum.user.dto.response.UserProfileResponse;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 사용자 프로필 이미지 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileImageService {

    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    private static final String DIRECTORY = "user";
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    /**
     * 프로필 이미지 업로드
     */
    @Transactional
    public UserProfileResponse uploadProfileImage(UUID userId, MultipartFile file) {
        validateImageFile(file);

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String currentImageUrl = user.getProfileImageUrl();
        String newImageUrl = imageStorageService.upload(file, DIRECTORY);

        user.updateProfileImage(newImageUrl);

        // Commit 성공 시에만 실행: Rollback시 Storage 영향 X
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // current Image를 저장소에서 삭제
                deleteStoredImage(currentImageUrl);
            }
        });

        return UserProfileResponse.from(user);
    }

    /**
     * 프로필 이미지 삭제
     */
    @Transactional
    public UserProfileResponse deleteProfileImage(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String currentImageUrl = user.getProfileImageUrl();
        user.clearProfileImage();

        // Commit 성공 시에만 실행: Rollback시 Storage 영향 X
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // current Image를 저장소에서 삭제
                deleteStoredImage(currentImageUrl);
            }
        });
        return UserProfileResponse.from(user);
    }

    /**
     * Storage에서 저장된 현재 이미지 삭제
     */
    public void deleteStoredImage(String imageUrl) {
        if (!isDeletableImage(imageUrl)) return;
        try {
            imageStorageService.delete(imageUrl);
        } catch (Exception e) {
            log.warn("프로필 이미지 파일 삭제 실패, 수동 정리 필요: {}", imageUrl, e);
        }
    }

    /**
     * 저장소에서 삭제 가능한 이미지 여부 확인 : null / 빈 문자열 -> false
     */
    private boolean isDeletableImage(String imageUrl) {
        return imageUrl != null && !imageUrl.isBlank();
    }

    /**
     * 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }
}