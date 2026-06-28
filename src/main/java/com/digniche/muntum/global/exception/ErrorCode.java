package com.digniche.muntum.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 모음
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * 400 ~
     */
    // 사용자 인증
    EMAIL_ALREADY_EXISTS("001", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),
    USER_NOT_FOUND("002", "존재하지 않는 사용자입니다", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD("003", "비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED),
    INACTIVE_ACCOUNT("004", "비활성화된 계정입니다", HttpStatus.FORBIDDEN),
    ACCESS_DENIED("0042", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("005", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("006", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED),
    NOT_AUTHENTICATED("000", "인증되지 않은 사용자 입니다", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("009", "Refresh Token이 존재하지 않습니다", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("010", "유효하지 않은 Refresh Token입니다", HttpStatus.UNAUTHORIZED),


    // 유효하지 않은 요청
    INVALID_REQUEST("007", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    // 프로그램
    PROGRAM_NOT_FOUND("100", "존재하지 않는 프로그램입니다", HttpStatus.NOT_FOUND),
    INVALID_PROGRAM_PERIOD("101", "프로그램 종료일은 시작일보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST),

    /**
     * 500 ~
     */
    // 서버
    SERVER_ERROR("008", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);


    private final String code;
    private final String message;
    private final HttpStatus status;
}
