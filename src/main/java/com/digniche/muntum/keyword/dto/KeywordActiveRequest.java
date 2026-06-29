package com.digniche.muntum.keyword.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 키워드 등록
 */
public record KeywordActiveRequest (
        @NotNull boolean active
){ }
