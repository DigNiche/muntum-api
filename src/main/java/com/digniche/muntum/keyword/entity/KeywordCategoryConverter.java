package com.digniche.muntum.keyword.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class KeywordCategoryConverter {

    private static final String SPLIT_CHAR = ",";
    private static final String DISPLAY_CHAR = ", ";

    // 키워드 등록 시, Literal 카테고리를 DISPLAY_CHAR 등을 이용하여 DB에 삽입 및 Display
    public static String convertToValidCategory(String rawCategories) {
        if (rawCategories == null || rawCategories.isBlank()) return "";
        Set<String> validCategoryDescription = Arrays.stream(KeywordCategory.values())
                .map(KeywordCategory::getDescription)
                .collect(Collectors.toSet());
        return Arrays.stream(rawCategories.split(SPLIT_CHAR))
                .map(String::trim)
                .filter(validCategoryDescription::contains)
                .collect(Collectors.joining(DISPLAY_CHAR));
    }
}
