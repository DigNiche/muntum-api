package com.digniche.muntum.curation.dto.request;

import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CurationChangeRequest(

        @NotBlank(message = "수정요청 사유는 필수입니다.")
        @Size(max = 500, message = "수정요청 사유는 500자를 넘을 수 없습니다.")
        String changeRequestReason,

        @NotNull(message = "공개 상태는 필수입니다.")
        CurationPublicationStatus publicationStatus

) {
}