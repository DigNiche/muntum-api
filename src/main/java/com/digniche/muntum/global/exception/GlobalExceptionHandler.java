package com.digniche.muntum.global.exception;

import com.digniche.muntum.global.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * DispatcherServlet 진입 후 내에서의 전반적인 예외 처리
 * - Controller
 * - Service
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ApiResponse.fail() 호출 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getStatus().value(), String.valueOf(errorCode), errorCode.getMessage()));
    }

    // @Valid 검증 실패 시 Spring이 throw하는 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다.");

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getStatus().value(), String.valueOf(errorCode), message));
    }

    // 이 외 모든 Catch 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ErrorCode errorCode = ErrorCode.SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getStatus().value(), String.valueOf(errorCode), errorCode.getMessage()));
    }
}
