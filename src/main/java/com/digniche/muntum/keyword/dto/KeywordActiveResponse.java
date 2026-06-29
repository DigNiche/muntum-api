package com.digniche.muntum.keyword.dto;

import com.digniche.muntum.keyword.entity.Keyword;

/**
 * Keyword 상태 변경 응답
 */
public record KeywordActiveResponse (
        boolean active,
        KeywordResponse keyword
) {
    public static KeywordActiveResponse from(Keyword keyword) {

        return new KeywordActiveResponse(
                keyword.isActive(),
                new KeywordResponse(
                    keyword.getId(),
                    keyword.getName(),
                    keyword.getDescription(),
                    keyword.getType().getDescription(),
                    keyword.getCategories()
                )
        );
    }
}
