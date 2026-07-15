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
        UUID informerId,
        String informerNickname,
        UUID reviewedById,
        String reviewedByNickname,
        LocalDateTime reviewedAt,
        String programName,
        String address,
        String reason,
        SuggestionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
    // FetchType.LAZY 필드가 있으므로 from은 반드시 서비스의 @Transactional 안에서 호출
    public static SpotSuggestionResponse from(SpotSuggestion suggestion) {
        return new SpotSuggestionResponse(
                suggestion.getId(),
                suggestion.getInformer() != null ? suggestion.getInformer().getId() : null,
                suggestion.getInformer() != null ? suggestion.getInformer().getNickname() : null,
                suggestion.getReviewedBy() != null ? suggestion.getReviewedBy().getId() : null,
                suggestion.getReviewedBy() != null ? suggestion.getReviewedBy().getNickname() : null,
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
