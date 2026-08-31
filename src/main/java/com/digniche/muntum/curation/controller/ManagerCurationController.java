package com.digniche.muntum.curation.controller;

import com.digniche.muntum.curation.dto.response.ManagerCurationDetailResponse;
import com.digniche.muntum.curation.dto.response.ManagerCurationListResponse;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.service.CurationService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@RequestMapping("/api/v1/manager/curations")
public class ManagerCurationController {

    private final CurationService curationService;

    /**
     * 관리자 큐레이션 심사 목록 조회
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    PageResponse<ManagerCurationListResponse>>>
    getCurations(
            @RequestParam(defaultValue = "PENDING")
            CurationStatus status,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size
    ) {
        PageResponse<ManagerCurationListResponse> response =
                curationService.getCurationsForManager(status, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("큐레이션 심사 목록 조회에 성공했습니다.", response)
        );
    }

    /**
     * 관리자 큐레이션 심사 상세 조회
     */
    @GetMapping("/{curation_id}")
    public ResponseEntity<
            ApiResponse<ManagerCurationDetailResponse>>
    getCuration(
            @PathVariable("curation_id")
            UUID curationId
    ) {
        ManagerCurationDetailResponse response =
                curationService.getCurationForManager(
                        curationId
                );

        return ResponseEntity.ok(
                ApiResponse.success("큐레이션 심사 상세 조회에 성공했습니다.", response)
        );
    }
}