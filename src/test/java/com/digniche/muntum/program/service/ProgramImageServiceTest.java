package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.storage.ImageStorageService;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 관리자 프로그램 이미지 개수(최소 1개, 최대 5개) 검증
 * - 등록(POST)·승인(approve-new)은 uploadImages, 수정(PUT)·이미지 교체(PATCH)는 replaceImages 경로
 * - 저장소·DB는 mock
 */
@ExtendWith(MockitoExtension.class)
class ProgramImageServiceTest {

    @Mock private ProgramImageRepository programImageRepository;

    @Mock private ImageStorageService imageStorageService;

    @InjectMocks private ProgramImageService programImageService;

    @Test
    void 이미지_5개_업로드는_순서대로_저장된다() {
        Program program = mock(Program.class);
        when(imageStorageService.upload(any(MultipartFile.class), eq("program")))
                .thenAnswer(invocation -> "https://cdn/" + ((MultipartFile) invocation.getArgument(0)).getOriginalFilename());

        programImageService.uploadImages(program, images(5));

        List<ProgramImage> saved = captureSavedImages();
        assertThat(saved).hasSize(5);
        assertThat(saved).extracting(ProgramImage::getDisplayOrder).containsExactly(1, 2, 3, 4, 5);
        assertThat(saved).extracting(ProgramImage::getImageUrl).containsExactly("https://cdn/1.png", "https://cdn/2.png", "https://cdn/3.png", "https://cdn/4.png", "https://cdn/5.png");
    }

    @Test
    void 이미지_6개_업로드는_I006_오류이고_저장소에_올리지_않는다() {
        Program program = mock(Program.class);

        assertThatThrownBy(() -> programImageService.uploadImages(program, images(6)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_PROGRAM_IMAGES);

        verifyNoInteractions(imageStorageService, programImageRepository);
    }

    @Test
    void 이미지_5개_교체는_기존_이미지를_지우고_새로_저장한다() {
        UUID programId = UUID.randomUUID();
        Program program = mock(Program.class);
        when(program.getId()).thenReturn(programId);
        when(imageStorageService.upload(any(MultipartFile.class), eq("program"))).thenReturn("https://cdn/new.png");
        ProgramImage oldImage = ProgramImage.builder().program(program).imageUrl("https://cdn/old.png").displayOrder(1).build();
        when(programImageRepository.findByProgramIdOrderByDisplayOrderAsc(programId)).thenReturn(List.of(oldImage));

        programImageService.replaceImages(program, images(5));

        verify(programImageRepository).deleteAllByProgramId(programId);
        verify(imageStorageService).delete("https://cdn/old.png");
        assertThat(captureSavedImages()).hasSize(5);
    }

    @Test
    void 이미지_6개_교체는_I006_오류이고_기존_이미지를_유지한다() {
        Program program = mock(Program.class);

        assertThatThrownBy(() -> programImageService.replaceImages(program, images(6)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_PROGRAM_IMAGES);

        verify(imageStorageService, never()).upload(any(), anyString());
        verify(programImageRepository, never()).deleteAllByProgramId(any());
    }

    @Test
    void 이미지_없이_업로드하면_I008_오류이고_저장소에_올리지_않는다() {
        Program program = mock(Program.class);

        assertThatThrownBy(() -> programImageService.uploadImages(program, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROGRAM_IMAGE_REQUIRED);
        assertThatThrownBy(() -> programImageService.uploadImages(program, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROGRAM_IMAGE_REQUIRED);

        verifyNoInteractions(imageStorageService, programImageRepository);
    }

    @Test
    void 이미지_없이_교체하면_I008_오류이고_기존_이미지를_유지한다() {
        Program program = mock(Program.class);

        assertThatThrownBy(() -> programImageService.replaceImages(program, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROGRAM_IMAGE_REQUIRED);
        assertThatThrownBy(() -> programImageService.replaceImages(program, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROGRAM_IMAGE_REQUIRED);

        verifyNoInteractions(imageStorageService, programImageRepository);
    }

    private List<MultipartFile> images(int count) {
        return IntStream.rangeClosed(1, count)
                .<MultipartFile>mapToObj(i -> new MockMultipartFile("images", i + ".png", "image/png", new byte[]{1}))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<ProgramImage> captureSavedImages() {
        ArgumentCaptor<List<ProgramImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(programImageRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
