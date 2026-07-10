package com.digniche.muntum.user.dto.request;

import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.UserTermsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 약관 등록 및 수정 요청 DTO
 */
public record TermsCreateRequest(

        @NotNull(message = "약관 유형은 필수입니다.")
        UserTermsType type,

        @NotBlank(message = "버전은 필수입니다.")
        @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "버전은 '1.0.13' 형식이어야 합니다.")
        @Size(max = 20, message = "버전은 20자를 넘을 수 없습니다.")
        String version,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
        String title,

        @NotBlank(message = "약관 내용은 필수입니다.")
        String content,

        @NotNull(message = "시행일은 필수입니다.")
        LocalDateTime effectiveAt
) {
    public Terms toEntity() {
        return Terms.builder()
                .type(type)
                .version(version)
                .title(title)
                .content(content)
                .effectiveAt(effectiveAt)
                .build();
    }
}