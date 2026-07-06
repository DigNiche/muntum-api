package com.digniche.muntum.global.exception;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.keyword.entity.KeywordCategory;
import com.digniche.muntum.keyword.entity.KeywordType;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.user.entity.UserTermsType;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
                .body(ApiResponse.fail(errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage()));
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
                .body(ApiResponse.fail(errorCode.getStatus().value(), errorCode.getCode(), message));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage()));
    }

    // Jackson의 역직렬화 실패 : Enum에 일치하지 않는 항목일 때 예외 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        String message = "";
        if (cause instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            Class<?> targetType = ife.getTargetType();

            if (targetType == UserTermsType.class) {
                message = "존재하지 않는 약관 항목입니다";
            } else if (targetType == UserRole.class) {
                message = "존재하지 않는 사용자 역할입니다";
            } else if (targetType == UserStatus.class) {
                message = "존재하지 않는 사용자 상태입니다";
            } else if (targetType == KeywordCategory.class) {
                message = "존재하지 않는 키워드 카테고리입니다";
            } else if (targetType == KeywordType.class) {
                message = "존재하지 않는 키워드 타입입니다";
            } else if (targetType == ProgramStatus.class) {
                message = "존재하지 않는 프로그램 상태입니다";
            } else if (targetType == ProgramType.class) {
                message = "존재하지 않는 프로그램 타입입니다";
            } else if (targetType == SuggestionStatus.class) {
                message = "존재하지 않는 제안 상태입니다";
            } else {
                message = "존재하지 않는 항목입니다";
            }
            // TODO: Enum 추가 시 작성
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(400, "INVALID_REQUEST", message));
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
