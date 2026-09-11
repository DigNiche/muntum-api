package com.digniche.muntum.curation.controller;

import com.digniche.muntum.curation.dto.response.PublicCurationDetailResponse;
import com.digniche.muntum.curation.dto.response.PublicCurationSummaryResponse;
import com.digniche.muntum.curation.service.PublicCurationQueryService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(
        "/api/v1/programs/{program_id}/curations"
)
public class ProgramCurationController {

    private final PublicCurationQueryService
            publicCurationQueryService;

    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    PageResponse<PublicCurationSummaryResponse>>>
    getCurations(
            @PathVariable("program_id")
            UUID programId,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        PageResponse<PublicCurationSummaryResponse> response =
                publicCurationQueryService.getCurations(
                        programId,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "프로그램 큐레이션 목록 조회에 성공했습니다.",
                        response
                )
        );
    }

    @GetMapping("/{curation_id}")
    public ResponseEntity<
            ApiResponse<PublicCurationDetailResponse>>
    getCuration(
            @PathVariable("program_id")
            UUID programId,

            @PathVariable("curation_id")
            UUID curationId
    ) {
        PublicCurationDetailResponse response =
                publicCurationQueryService.getCuration(
                        programId,
                        curationId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "큐레이션 상세 조회에 성공했습니다.",
                        response
                )
        );
    }
}