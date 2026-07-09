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

    // 비밀번호 재설정
    VERIFICATION_CODE_EXPIRED("A011", "인증번호가 만료되었습니다. 다시 요청해주세요", HttpStatus.BAD_REQUEST),
    INVALID_VERIFICATION_CODE("A012", "인증번호가 일치하지 않습니다", HttpStatus.BAD_REQUEST),
    TOO_MANY_VERIFICATION_ATTEMPTS("A013", "인증 시도 횟수를 초과했습니다. 다시 요청해주세요", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_RESET_TOKEN("A014", "유효하지 않거나 만료된 요청입니다. 처음부터 다시 시도해주세요", HttpStatus.BAD_REQUEST),

    // 사용자 정보
    NICKNAME_ALREADY_EXISTS("U001", "이미 사용 중인 닉네임입니다", HttpStatus.CONFLICT),

    // 사용자 약관 동의
    TERMS_NOT_FOUND("T001", "약관 동의 정보가 존재하지 않습니다", HttpStatus.NOT_FOUND),
    REQUIRED_TERMS_CANNOT_DISAGREE("T002", "필수 약관은 철회할 수 없습니다", HttpStatus.BAD_REQUEST),
    REQUIRED_TERMS_NOT_AGREED("T003", "필수 약관에 동의하지 않았습니다", HttpStatus.BAD_REQUEST),

    // 프로그램
    PROGRAM_NOT_FOUND("P001", "존재하지 않는 프로그램입니다", HttpStatus.NOT_FOUND),
    INVALID_PROGRAM_PERIOD("P002", "프로그램 종료일은 시작일보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_SEARCH_CONDITION("P003", "검색어 검색과 키워드 검색은 동시에 사용할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ACCESS_SECTION("P004", "Hot 필터 칩은 지도에서만 사용할 수 있습니다.", HttpStatus.BAD_REQUEST),

    // 프로그램 이미지
    PROGRAM_IMAGE_NOT_FOUND("I001", "존재하지 않는 이미지입니다", HttpStatus.NOT_FOUND),
    IMAGE_UPLOAD_FAILED("I002", "이미지 업로드에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_IMAGE_FILE("I003", "유효하지 않은 이미지 파일입니다", HttpStatus.BAD_REQUEST),
    TOO_MANY_PROGRAM_IMAGES("I004", "이미지는 최대 5개까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST),

    // 카카오 GeoCoordinate
    ADDRESS_NOT_FOUD("G001", "주소를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 키워드
    KEYWORD_NOT_FOUND("K001", "존재하지 않는 키워드입니다", HttpStatus.BAD_REQUEST),
    KEYWORD_ALREADY_EXISTS("K002", "이미 존재하는 키워드입니다", HttpStatus.CONFLICT),
    INVALID_KEYWORD_TYPE("K003", "유효하지 않은 키워드 타입입니다", HttpStatus.BAD_REQUEST),

    // 스크랩
    SCRAP_NOT_FOUND("C001", "존재하지 않는 스크랩입니다", HttpStatus.NOT_FOUND),

    // 제보
    SUGGESTION_NOT_FOUND("S001", "존재하지 않는 제보입니다", HttpStatus.NOT_FOUND),
    SUGGESTION_ACCESS_DENIED("S002", "해당 제보에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),
    SUGGESTION_NOT_EDITABLE("S003", "검토가 시작된 제보는 수정할 수 없습니다", HttpStatus.CONFLICT),
    INVALID_SUGGESTION_STATUS_TRANSITION("S004", "허용되지 않는 상태 변경입니다", HttpStatus.BAD_REQUEST),

    // 공지사항
    ANNOUNCEMENT_NOT_FOUND("AN001", "존재하지 않는 공지사항입니다", HttpStatus.NOT_FOUND),
    ANNOUNCEMENT_ACCESS_DENIED("S002", "공지사항에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),

    // 유효하지 않은 요청
    INVALID_REQUEST("007", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("G002", "업로드 가능한 파일 크기를 초과했습니다", HttpStatus.PAYLOAD_TOO_LARGE),

    // 500~
    SERVER_ERROR("G001", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);


    private final String code;
    private final String message;
    private final HttpStatus status;
}
