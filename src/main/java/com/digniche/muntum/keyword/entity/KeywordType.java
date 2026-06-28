package com.digniche.muntum.keyword.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 키워드 유형(대분류)
 */
@Getter
@RequiredArgsConstructor
public enum KeywordType {
    THEME("테마"),
    SUBJECTSITUATION("주제"),
    SITUATION("상황");

    private final String type_kr;
}