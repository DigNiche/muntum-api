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

    // 사용자 인증
    EMAIL_ALREADY_EXISTS("A001", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),
    USER_NOT_FOUND("A002", "존재하지 않는 사용자입니다", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD("A003", "비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED),
    INACTIVE_ACCOUNT("A004", "비활성화된 계정입니다", HttpStatus.FORBIDDEN),
    ACCESS_DENIED("A005", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("A006", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("A007", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED),
    NOT_AUTHENTICATED("A008", "인증되지 않은 사용자 입니다", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("A009", "Refresh Token이 존재하지 않습니다", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("A010", "유효하지 않은 Refresh Token입니다", HttpStatus.UNAUTHORIZED),

    // 사용자 정보
    NICKNAME_ALREADY_EXISTS("U001", "이미 사용 중인 닉네임입니다", HttpStatus.CONFLICT),

    // 사용자 약관 동의
    TERMS_NOT_FOUND("T001", "약관 동의 정보가 존재하지 않습니다", HttpStatus.NOT_FOUND),
    REQUIRED_TERMS_CANNOT_DISAGREE("T002", "필수 약관은 철회할 수 없습니다", HttpStatus.BAD_REQUEST),
    REQUIRED_TERMS_NOT_AGREED("T003", "필수 약관에 동의하지 않았습니다", HttpStatus.BAD_REQUEST),

    // 프로그램
    PROGRAM_NOT_FOUND("P001", "존재하지 않는 프로그램입니다", HttpStatus.NOT_FOUND),
    INVALID_PROGRAM_PERIOD("P002", "프로그램 종료일은 시작일보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 키워드
    KEYWORD_NOT_FOUND("K001", "존재하지 않는 키워드입니다", HttpStatus.BAD_REQUEST),
    KEYWORD_ALREADY_EXISTS("K002", "이미 존재하는 키워드입니다", HttpStatus.CONFLICT),
    INVALID_KEYWORD_TYPE("K003", "유효하지 않은 키워드 타입입니다", HttpStatus.BAD_REQUEST),

    // 유효하지 않은 요청
    INVALID_REQUEST("007", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),

    // 500~
    SERVER_ERROR("5001", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);


    private final String code;
    private final String message;
    private final HttpStatus status;
}
