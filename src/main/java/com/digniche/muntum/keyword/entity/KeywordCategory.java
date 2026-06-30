package com.digniche.muntum.keyword.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum KeywordCategory {
    EXPERIENCE("체험/활동"),
    FESTIVAL("축제/활발"),
    AESTHETIC("심미/예술"),
    EDUCATION("교육"),
    IMMERSION("몰입/감상"),
    TRENDY("트렌디/SNS"),
    CALM("차분/사색"),
    SITUATION("상황");

    private final String description;

    // 키워드 등록 시, Literal 카테고리를 DISPLAY_CHAR 등을 이용하여 DB에 삽입 및 Display
    public static String validateCategories(String rawCategories) {
        String splitChar = ", ";
        String concationChar = ", ";

        if (rawCategories == null || rawCategories.isBlank()) return "";
        Set<String> validCategoryDescription = Arrays.stream(values())
                .map(KeywordCategory::getDescription)
                .collect(Collectors.toSet());
        return Arrays.stream(rawCategories.split(splitChar))
                .map(String::trim)
                .filter(validCategoryDescription::contains)
                .collect(Collectors.joining(concationChar));
    }
}
