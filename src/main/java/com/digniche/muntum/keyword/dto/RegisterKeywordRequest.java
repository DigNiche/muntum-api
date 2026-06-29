package com.digniche.muntum.keyword.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 키워드 등록
 */
public record RegisterKeywordRequest (
        @NotBlank @Size(max = 50) String name,
        @Size(max = 255) String description,
        @NotBlank String type,
        String categories
) { }
