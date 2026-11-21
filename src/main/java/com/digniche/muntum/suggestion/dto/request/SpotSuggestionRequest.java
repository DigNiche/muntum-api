package com.digniche.muntum.suggestion.dto.request;

import com.digniche.muntum.suggestion.entity.SpotSuggestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 제보 등록/수정 Request
 */
public record SpotSuggestionRequest(
        @NotBlank @Size(max = 100) String programName,
        @Size(max = 255) String address,
        @Size(max = 2000) String reason
) {
    public SpotSuggestion toEntity(UUID informer) {
        return SpotSuggestion.builder()
                .informer(informer)
                .programName(programName)
                .address(address)
                .reason(reason)
                .build();
    }
}
