package com.digniche.muntum.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Auth 에러 코드 모음
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    // 사용자 인증
    INVALID_REFRESH_TOKEN("AUTH005", "유효하지 않은 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_REFRESH_TOKEN("AUTH006", "만료된 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
