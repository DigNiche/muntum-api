package com.digniche.muntum.keyword.dto;

import java.util.List;

/**
 * 키워드 설정 Response
 */
public record SelectedKeywordsResponse (
        List<KeywordResponse> selectedKeywords
) {}