package com.digniche.muntum.curation.controller;

import com.digniche.muntum.curation.dto.request.CurationCreateRequest;
import com.digniche.muntum.curation.dto.response.CurationDetailResponse;
import com.digniche.muntum.curation.dto.response.CurationListResponse;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.service.CurationService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.digniche.muntum.curation.dto.request.CurationUpdateRequest;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/curations")
public class CurationController {
    private final CurationService curationService;
    /**
     * 큐레이션 등록
     */
    @PreAuthorize("hasRole('CURATOR')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CurationDetailResponse>>
    createCuration(
            @AuthenticationPrincipal
            UserPrincipal userPrincipal,

            @RequestPart("curation")
            @Valid
            CurationCreateRequest request,

            @RequestPart(value = "images", required = false)
            List<MultipartFile> files
    ) {
        CurationDetailResponse response = curationService.createCuration(userPrincipal.getUserId(), request, files);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("큐레이션이 등록되었습니다.", response));
    }

    /**
     * 내 큐레이션 목록 조회
     */
    @PreAuthorize("hasRole('CURATOR')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<CurationListResponse>>> getMyCurations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,

            @RequestParam(required = false) CurationStatus status,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size
    ) {
        PageResponse<CurationListResponse> response =
                curationService.getMyCurations(userPrincipal.getUserId(), status, page, size);

        return ResponseEntity.ok(ApiResponse.success("내 큐레이션 목록 조회에 성공했습니다.", response));
    }

    /**
     * 내 큐레이션 상세 조회
     */
    @PreAuthorize("hasRole('CURATOR')")
    @GetMapping("/me/{curation_id}")
    public ResponseEntity<ApiResponse<CurationDetailResponse>> getMyCuration(
            @PathVariable("curation_id")
            UUID curationId,

            @AuthenticationPrincipal
            UserPrincipal userPrincipal
    ) {
        CurationDetailResponse response = curationService.getMyCuration(curationId, userPrincipal.getUserId());

        return ResponseEntity.ok(ApiResponse.success("내 큐레이션 상세 조회에 성공했습니다.", response)
        );
    }

    /**
     * 큐레이션 수정
     */
    @PreAuthorize("hasRole('CURATOR')")
    @PutMapping(
            value = "/{curation_id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<CurationDetailResponse>>
    updateCuration(
            @PathVariable("curation_id")
            UUID curationId,

            @AuthenticationPrincipal
            UserPrincipal userPrincipal,

            @RequestPart("curation")
            @Valid
            CurationUpdateRequest request,

            @RequestPart(
                    value = "images",
                    required = false
            )
            List<MultipartFile> files
    ) {
        CurationDetailResponse response =
                curationService.updateCuration(
                        curationId,
                        userPrincipal.getUserId(),
                        request,
                        files
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "큐레이션이 수정되었습니다.",
                        response
                )
        );
    }

    /**
     * 큐레이션 삭제
     */

    @PreAuthorize("hasRole('CURATOR')")
    @DeleteMapping("/{curation_id}")
    public ResponseEntity<ApiResponse<Void>>
    deleteCuration(
            @PathVariable("curation_id")
            UUID curationId,

            @AuthenticationPrincipal
            UserPrincipal userPrincipal
    ) {
        curationService.deleteCuration(
                curationId,
                userPrincipal.getUserId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "큐레이션이 삭제되었습니다.",
                        null
                )
        );
    }

    /**
     * 재등록
     */
    @PreAuthorize("hasRole('CURATOR')")
    @PatchMapping("/{curation_id}/resubmit")
    public ResponseEntity<ApiResponse<CurationDetailResponse>>
    resubmitCuration(
            @PathVariable("curation_id")
            UUID curationId,

            @AuthenticationPrincipal
            UserPrincipal userPrincipal
    ) {
        CurationDetailResponse response =
                curationService.resubmitCuration(
                        curationId,
                        userPrincipal.getUserId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "큐레이션이 다시 등록되었습니다.",
                        response
                )
        );
    }

    @PatchMapping("/{curationId}/unpublish")
    public ResponseEntity<ApiResponse<Void>> unpublishCuration(
            @PathVariable UUID curationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        curationService.unpublishCuration(
                curationId,
                userPrincipal.getUserId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "큐레이션 공개가 중단되었습니다.",
                        null
                )
        );
    }
}

