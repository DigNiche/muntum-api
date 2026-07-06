package com.digniche.muntum.suggestion.dto.response;

import com.digniche.muntum.suggestion.entity.SpotSuggestion;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제보 등록/수정 Response
 */
public record SpotSuggestionResponse(
        UUID id,
        UUID informerId, String informerNickname,
        UUID reviewedById, String reviewedByNickname,
        LocalDateTime reviewedAt,
        String programName, String address, String reason,
        SuggestionStatus status,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static SpotSuggestionResponse from(SpotSuggestion suggestion) {
        // informer/reviewedBy는 유저 탈퇴 cascade로 null이 될 수 있으므로 null-check 필수

    }
}
