package com.digniche.muntum.program.service;

import com.digniche.muntum.global.storage.ImageStorageService;
import com.digniche.muntum.program.dto.response.ProgramImageResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 프로그램 이미지 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramImageService {

    private final ProgramImageRepository programImageRepository;
    private final ImageStorageService imageStorageService;

    private static final String DIRECTORY = "program";
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    // 이미지 업로드
    @Transactional
    public List<ProgramImageResponse> uploadImages(UUID programId, List<MultipartFile> files) {
        Program program = getProgram(programId);

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

        return programImageRepository.saveAll(images).stream()
                .map(ProgramImageResponse::from)
                .toList();
    }
}
