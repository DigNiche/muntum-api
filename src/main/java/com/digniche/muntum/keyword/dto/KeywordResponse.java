package com.digniche.muntum.keyword.dto;

import com.digniche.muntum.keyword.entity.Keyword;

import java.util.UUID;

public record KeywordResponse(
        UUID id,
        String name,
        String description,
        String type,
        String categories

) {
    public static KeywordResponse from(Keyword keyword) {
        return new KeywordResponse(
                keyword.getId(),
                keyword.getName(),
                keyword.getDescription(),
                keyword.getType().getDescription(),
                keyword.getCategories()
        );
    }
}
