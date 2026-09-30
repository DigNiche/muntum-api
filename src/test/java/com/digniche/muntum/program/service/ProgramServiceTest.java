package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.repository.ProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 프로그램 생성 공통 로직(등록·신규 프로그램 승인)의 이미지 선검증
 * - 이미지 검증 실패 시 지오코딩 호출·프로그램 저장 전에 종료되는지 확인
 */
@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock private ProgramRepository programRepository;

    @Mock private ProgramImageService programImageService;

    @Mock private GeocodingService geocodingService;

    @InjectMocks private ProgramService programService;

    @Test
    void 이미지_검증에_실패하면_지오코딩과_저장_없이_종료된다() {
        ProgramCreateRequest request = mock(ProgramCreateRequest.class);
        doThrow(new BusinessException(ErrorCode.PROGRAM_IMAGE_REQUIRED))
                .when(programImageService).validateImageFiles(null);

        assertThatThrownBy(() -> programService.createProgramWithAssets(request, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROGRAM_IMAGE_REQUIRED);

        verifyNoInteractions(request, geocodingService, programRepository);
    }
}
