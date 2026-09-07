package com.digniche.muntum.curator.dto.request;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 큐레이터 지원서 작성 DTO
 */
public record CuratorApplicationCreateRequest(

        @NotBlank
        @Size(max = 100)
        String programName,

        @NotBlank
        @Size(max = 255)
        String tagline,

        @NotBlank
        String curation
) {
    public CuratorApplication toEntity(User applicant) {
        return CuratorApplication.builder()
                .applicant(applicant)
                .programName(programName)
                .tagline(tagline)
                .curation(curation)
                .build();
    }
}