package com.digniche.muntum.curation.controller;

import com.digniche.muntum.curation.dto.request.CurationApproveExistingRequest;
import com.digniche.muntum.curation.dto.request.CurationChangeRequest;
import com.digniche.muntum.curation.dto.response.ManagerCurationDetailResponse;
import com.digniche.muntum.curation.dto.response.ManagerCurationListResponse;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.service.CurationService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
    /**
     * 기존 프로그램 연결 승인
     */
    @PatchMapping("/{curation_id}/approve-existing")
    public ResponseEntity<
            ApiResponse<ManagerCurationDetailResponse>>
    approveExisting(
            @PathVariable("curation_id")
            UUID curationId,

            @AuthenticationPrincipal
            UserPrincipal userPrincipal,

            @RequestBody
            @Valid
            CurationApproveExistingRequest request
    ) {
        ManagerCurationDetailResponse response =
                curationService.approveExisting(
                        curationId,
                        userPrincipal.getUserId(),
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "기존 프로그램에 연결하여 큐레이션을 승인했습니다.",
                        response
                )
        );
    }

    /**
     * 큐레이션 반려
     */
    @PatchMapping("/{curation_id}/request-changes")
    public ResponseEntity<
            ApiResponse<ManagerCurationDetailResponse>>
    requestChanges(
            @PathVariable("curation_id")
            UUID curationId,

            @AuthenticationPrincipal
            UserPrincipal userPrincipal,

            @RequestBody
            @Valid
            CurationChangeRequest request
    ) {
        ManagerCurationDetailResponse response = curationService.requestChanges(curationId, userPrincipal.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("큐레이션이 반려되었습니다.", response)
        );
    }

    /**
     * 신규 프로그램 생성 후 큐레이션 승인
     */
    @PostMapping(
            value = "/{curation_id}/approve-new",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<
            ApiResponse<ManagerCurationDetailResponse>>
    approveNew(
            @PathVariable("curation_id")
            UUID curationId,

            @AuthenticationPrincipal
            UserPrincipal userPrincipal,

            @RequestPart("program")
            @Valid
            ProgramCreateRequest request,

            @RequestPart(
                    value = "images",
                    required = false
            )
            List<MultipartFile> files
    ) {
        ManagerCurationDetailResponse response =
                curationService.approveNew(curationId, userPrincipal.getUserId(), request, files);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로그램을 등록하고 큐레이션을 승인했습니다.", response));
    }
}