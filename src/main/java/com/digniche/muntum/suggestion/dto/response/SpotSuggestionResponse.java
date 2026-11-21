package com.digniche.muntum.suggestion.dto.response;

import com.digniche.muntum.global.auditor.UserDisplay;
import com.digniche.muntum.suggestion.entity.SpotSuggestion;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제보 등록/수정 Response
 */
public record SpotSuggestionResponse(
        UUID id,
        UUID informerId,
        String informerNickname,
        UUID reviewerId,
        String reviewerNickname,
        LocalDateTime reviewedAt,
        String programName,
        String address,
        String reason,
        SuggestionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
    // informer, reviewer: 제보자·검토자 표시 값(UserDisplayResolver 결과). 값이 없으면 null
    public static SpotSuggestionResponse from(SpotSuggestion suggestion, UserDisplay informer, UserDisplay reviewer) {
        return new SpotSuggestionResponse(
                suggestion.getId(),
                informer != null ? informer.id() : null,
                informer != null ? informer.nickname() : null,
                reviewer != null ? reviewer.id() : null,
                reviewer != null ? reviewer.nickname() : null,
                suggestion.getReviewedAt(),
                suggestion.getProgramName(),
                suggestion.getAddress(),
                suggestion.getReason(),
                suggestion.getStatus(),
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt(),
                suggestion.isDeleted()
        );
    }
}
