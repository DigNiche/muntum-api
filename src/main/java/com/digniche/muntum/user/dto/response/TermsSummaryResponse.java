package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.UserTermsType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 약관 목록용 요약 응답 (content 제외)
 */
public record TermsSummaryResponse(
        UUID id,
        UserTermsType type,
        boolean required,
        String version,
        String title,
        LocalDateTime effectiveAt,
        boolean active
) {
    public static TermsSummaryResponse from(Terms terms) {
        return new TermsSummaryResponse(
                terms.getId(),
                terms.getType(),
                terms.getType().isRequired(),
                terms.getVersion(),
                terms.getTitle(),
                terms.getEffectiveAt(),
                terms.isActive()
        );
    }
}
