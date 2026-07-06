package com.digniche.muntum.keyword.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 키워드 선택 Request
 */
public record SelectKeywordsRequest(
        @NotEmpty @Size(min = 3)
        List<@NotBlank String> selectKeywords
) {}
