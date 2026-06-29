package com.digniche.muntum.keyword.entity;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 키워드 유형(대분류)
 */
@Getter
@RequiredArgsConstructor
public enum KeywordType {
    THEME("테마"),
    SUBJECT("주제"),
    SITUATION("상황");

    private final String description;

    // String 파라미터 description과 일치하는 Enum 타입을 반환
    public static KeywordType validateType(String description) {
        return Arrays.stream(values())
                .filter(t -> t.description.equals(description))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_KEYWORD_TYPE));
    }
}