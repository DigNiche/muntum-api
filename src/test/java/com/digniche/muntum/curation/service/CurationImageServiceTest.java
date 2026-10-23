package com.digniche.muntum.curation.service;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationImage;
import com.digniche.muntum.curation.repository.CurationImageRepository;
import com.digniche.muntum.curation.dto.response.CurationImageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.storage.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

/**
 * 큐레이션 이미지 개수(선택 입력, 최대 5개) 검증
 * - 등록(POST)은 uploadImages 경로
 * - 저장소·DB는 mock
 */
@ExtendWith(MockitoExtension.class)
class CurationImageServiceTest {

    @Mock private CurationImageRepository curationImageRepository;

    @Mock private ImageStorageService imageStorageService;

    @InjectMocks private CurationImageService curationImageService;

    @Test
    void 이미지_없이_업로드하면_빈_목록을_반환하고_아무것도_저장하지_않는다() {
        Curation curation = mock(Curation.class);

        assertThat(curationImageService.uploadImages(curation, null)).isEmpty();
        assertThat(curationImageService.uploadImages(curation, List.of())).isEmpty();

        verifyNoInteractions(imageStorageService, curationImageRepository);
    }

    @Test
    void 이미지_5개_업로드는_순서대로_저장하고_저장_결과를_반환한다() {
        Curation curation = mock(Curation.class);
        when(imageStorageService.upload(any(MultipartFile.class), eq("curation")))
                .thenAnswer(invocation -> "https://cdn/" + ((MultipartFile) invocation.getArgument(0)).getOriginalFilename());
        when(curationImageRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CurationImageResponse> responses = curationImageService.uploadImages(curation, images(5));

        List<CurationImage> saved = captureSavedImages();
        assertThat(saved).extracting(CurationImage::getDisplayOrder).containsExactly(1, 2, 3, 4, 5);
        assertThat(responses).extracting(CurationImageResponse::displayOrder).containsExactly(1, 2, 3, 4, 5);
        assertThat(responses).extracting(CurationImageResponse::imageUrl)
                .containsExactly("https://cdn/1.png", "https://cdn/2.png", "https://cdn/3.png", "https://cdn/4.png", "https://cdn/5.png");
    }

    @Test
    void 이미지_6개_업로드는_CU007_오류이고_저장소에_올리지_않는다() {
        Curation curation = mock(Curation.class);

        assertThatThrownBy(() -> curationImageService.uploadImages(curation, images(6)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_CURATION_IMAGES);

        verifyNoInteractions(imageStorageService, curationImageRepository);
    }

    private List<MultipartFile> images(int count) {
        return IntStream.rangeClosed(1, count)
                .<MultipartFile>mapToObj(i -> new MockMultipartFile("images", i + ".png", "image/png", new byte[]{1}))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<CurationImage> captureSavedImages() {
        ArgumentCaptor<List<CurationImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(curationImageRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
