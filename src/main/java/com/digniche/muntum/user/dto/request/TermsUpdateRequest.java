package com.digniche.muntum.user.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 약관 수정 요청 DTO : 게시 전 오타/내용 수정용
 */
public record TermsUpdateRequest(

        @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
        String title,

        String content,

        LocalDateTime effectiveAt
) {
}
