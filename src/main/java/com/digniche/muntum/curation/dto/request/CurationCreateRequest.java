package com.digniche.muntum.curation.dto.request;

import com.digniche.muntum.curation.entity.Curation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CurationCreateRequest (
    @NotBlank
    @Size(max = 100)
    String submittedProgramTitle,

    @NotBlank @Size(max = 255)
    String submittedPlace,

    @NotBlank @Size(max = 255)
    String tagline,

    @NotBlank
    String content
) {
    public Curation toEntity(UUID curatorId) {
        return Curation.builder()
                .curatorId(curatorId)
                .submittedProgramTitle(submittedProgramTitle)
                .submittedPlace(submittedPlace)
                .tagline(tagline)
                .content(content)
                .build();
    }
}
