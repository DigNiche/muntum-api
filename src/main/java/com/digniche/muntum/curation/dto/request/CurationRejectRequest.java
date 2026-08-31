package com.digniche.muntum.curation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CurationRejectRequest(

        @NotBlank(message = "반려 사유는 필수입니다.")
        @Size(max = 500, message = "반려 사유는 500자를 넘을 수 없습니다.")
        String rejectionReason

) {
}