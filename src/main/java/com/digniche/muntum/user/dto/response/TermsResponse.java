package com.digniche.muntum.user.dto.response;

import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.UserTermsType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 약관 응답 DTO
 */
public record TermsResponse(
        UUID id,
        UserTermsType type,
        boolean required,
        String version,
        String title,
        String content,
        LocalDateTime effectiveAt,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TermsResponse from(Terms terms) {
        return new TermsResponse(
                terms.getId(),
                terms.getType(),
                terms.getType().isRequired(),   // Flutter가 필수/선택 UI 분기할 때 사용
                terms.getVersion(),
                terms.getTitle(),
                terms.getContent(),
                terms.getEffectiveAt(),
                terms.isActive(),
                terms.getCreatedAt(),
                terms.getUpdatedAt()
        );
    }
}