package com.digniche.muntum.program.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.dto.request.ProgramUpdateRequest;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.program.dto.response.ProgramImageResponse;
import com.digniche.muntum.program.dto.response.ProgramResponse;
import com.digniche.muntum.program.service.ProgramImageService;
import com.digniche.muntum.program.service.ProgramService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.digniche.muntum.global.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 프로그램 API 컨트롤러
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/programs")
public class ProgramController {

    private final ProgramService programService;
    private final ProgramImageService programImageService;

    /**
     * 프로그램 등록 (큐레이터, 관리자)
     */
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgramResponse>> createProgram(
//            @Valid @RequestBody ProgramCreateRequest request
            @RequestPart("program") @Valid ProgramCreateRequest request,
            @RequestPart(value="images", required=false) List<MultipartFile> files) {
        ProgramResponse response = programService.createProgram(request, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로그램이 등록되었습니다.", response));
    }

    /**
     * 프로그램 목록 조회
     */
    @GetMapping("")
     public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getPrograms(
             @PageableDefault(size = 20) Pageable pageable
     ) {
        PageResponse<ProgramCardResponse> response = programService.getPrograms(pageable);
         return ResponseEntity.ok(ApiResponse.success("프로그램 목록 조회에 성공했습니다.", response));

//    public ApiResponse<PageResponse<ProgramListResponse>> getPrograms(
//            @RequestParam(defaultValue = "LATEST") ProgramSortType sort,
//            @RequestParam(defaultValue = "DESC") Sort.Direction order,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size
//    ) {
//        PageResponse<ProgramListResponse> response =
//                programService.getPrograms(sort, order, page, size);
//
//        return ApiResponse.success("프로그램 목록 조회에 성공했습니다.", response);
    }

    // 마감일이 이번달인 목록 중 마감일이 오늘 날짜와 가까운 순으로 정렬
    @GetMapping("/closing-soon")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getProgramsByClosestEndDate(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramCardResponse> response = programService.getProgramsByClosestEndDate(pageable);
        return ResponseEntity.ok(ApiResponse.success("마감 임박 프로그램 목록 조회에 성공했습니다.", response));
    }

    // 인기 키워드를 가진 프로그램 목록 정렬
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getHotPrograms(
            @RequestParam(defaultValue = "5") @Min(1) int topN,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramCardResponse> response = programService.getProgramsByHotKeywords(topN, pageable);
        return ResponseEntity.ok(ApiResponse.success("인기 키워드 프로그램 목록 조회에 성공했습니다.", response));
    }



    /**
     * 프로그램 단건 조회
     */
    @GetMapping("/{program_id}")
    public ResponseEntity<ApiResponse<ProgramResponse>> getProgram(
            @PathVariable("program_id") UUID programId
    ) {
        ProgramResponse response = programService.getProgram(programId);
        return ResponseEntity.ok(ApiResponse.success("프로그램 조회에 성공했습니다.", response));
    }

    /**
     * 프로그램 수정 (큐레이터, 관리자)
     */
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PutMapping("/{program_id}")
    public ResponseEntity<ApiResponse<ProgramResponse>> updateProgram(
            @PathVariable("program_id") UUID programId,
            @Valid @RequestBody ProgramUpdateRequest request
    ) {
        ProgramResponse response = programService.updateProgram(programId, request);
        return ResponseEntity.ok(ApiResponse.success("프로그램이 수정되었습니다.", response));
    }

    /**
     * 프로그램 삭제 (관리자)
     */
    @PreAuthorize("hasAnyRole('MANAGER')")
    @DeleteMapping("/{program_id}")
    public ResponseEntity<ApiResponse<Void>> deleteProgram(
            @PathVariable("program_id") UUID programId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        programService.deleteProgram(programId, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("프로그램이 삭제되었습니다.", null));
    }

    /**
     * 프로그램 썸네일 이미지 목록 조회
     */
    @GetMapping("/thumbnails")
    public ResponseEntity<ApiResponse<List<ProgramImageResponse>>> getThumbnails() {
        return ResponseEntity.ok(ApiResponse.success("썸네일 목록 조회에 성공했습니다.", programImageService.getThumbnails()));
    }

}