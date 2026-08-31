package com.digniche.muntum.curation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CurationUpdateRequest(

        @NotBlank(message = "프로그램명은 필수입니다.")
        @Size(max = 100, message = "프로그램명은 100자를 넘을 수 없습니다.")
        String submittedProgramTitle,

        @NotBlank(message = "장소는 필수입니다.")
        @Size(max = 255, message = "장소는 255자를 넘을 수 없습니다.")
        String submittedPlace,

        @NotBlank(message = "한 줄 소개는 필수입니다.")
        @Size(max = 255, message = "한 줄 소개는 255자를 넘을 수 없습니다.")
        String tagline,

        @NotBlank(message = "큐레이션 본문은 필수입니다.")
        String content

) {
}