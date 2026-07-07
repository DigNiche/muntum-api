package com.digniche.muntum.suggestion.dto.request;

import com.digniche.muntum.suggestion.entity.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 제보 상태 변경 Request
 */
public record SuggestionStatusUpdateRequest(
        @NotNull SuggestionStatus status
) {}
