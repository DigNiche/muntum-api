package com.digniche.muntum.global.exception;

import com.digniche.muntum.global.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 요청 형식(Content-Type) 불일치 응답
 * - multipart API에 raw JSON을 보내는 경우 500이 아닌 415(G003)
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 지원하지_않는_요청_형식은_415_G003과_지원_형식을_응답한다() {
        HttpMediaTypeNotSupportedException e = new HttpMediaTypeNotSupportedException(
                MediaType.APPLICATION_JSON, List.of(MediaType.MULTIPART_FORM_DATA));

        ResponseEntity<ApiResponse<Void>> response = handler.handleHttpMediaTypeNotSupported(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().getStatus()).isEqualTo(415);
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
        assertThat(response.getBody().getMessage())
                .startsWith(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage())
                .contains("multipart/form-data");
    }

    @Test
    void 지원_형식_정보가_없으면_기본_메시지만_응답한다() {
        HttpMediaTypeNotSupportedException e = new HttpMediaTypeNotSupportedException("Content-Type not supported");

        ResponseEntity<ApiResponse<Void>> response = handler.handleHttpMediaTypeNotSupported(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage());
    }
}
