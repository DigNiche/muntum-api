package com.digniche.muntum.global;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 공통 포장 DTO
 * Spring의 Page를 그대로 노출하지 않고, 필요한 정보만 추려서 반환한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext()
        );
    }
}
