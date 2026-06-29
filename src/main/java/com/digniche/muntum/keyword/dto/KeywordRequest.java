package com.digniche.muntum.keyword.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 키워드 등록
 */
public record KeywordRequest(
        @NotBlank @NotNull @Size(max = 50) String name,
        @Size(max = 255) String description,
        String type,
        String categories
) { }
