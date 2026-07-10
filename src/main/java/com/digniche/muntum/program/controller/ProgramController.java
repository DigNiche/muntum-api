package com.digniche.muntum.program.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.program.dto.request.*;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.dto.response.ProgramImageResponse;
import com.digniche.muntum.program.dto.response.ProgramResponse;
import com.digniche.muntum.program.service.ProgramImageService;
import com.digniche.muntum.program.service.ProgramService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // 프로그램 등록
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgramResponse>> registerProgram(
            @RequestPart("program") @Valid ProgramCreateRequest request,
            @RequestPart(value="images", required=false) List<MultipartFile> files) {
        ProgramResponse response = programService.createProgram(request, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로그램이 등록되었습니다.", response));
    }

    // 프로그램 수정
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PutMapping(value = "/{program_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgramResponse>> rewriteProgram(
            @PathVariable("program_id") UUID programId,
            @RequestPart("program") @Valid ProgramUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> files
    ) {
        ProgramResponse response = programService.updateProgram(programId, request, files);
        return ResponseEntity.ok(ApiResponse.success("프로그램이 수정되었습니다.", response));
    }

    // 프로그램 이미지 수정 (전체 교체. null이면 전체 삭제로 동작)
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PatchMapping(value = "/{program_id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ProgramImageResponse>>> updateProgramImages(
            @PathVariable("program_id") UUID programId,
            @RequestPart(value = "images", required = false) List<MultipartFile> files
    ) {
        List<ProgramImageResponse> response = programService.updateProgramImages(programId, files);
        return ResponseEntity.ok(ApiResponse.success("프로그램 이미지가 수정되었습니다.", response));
    }


    // 프로그램 삭제
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
     * 프로그램 목록 조회 + 검색 (텍스트 / 키워드) 통합 진입점
     * - search : 텍스트 검색 (프로그램명/한줄소개/큐레이션) — 다음 단계 구현
     * - keywordIds : 키워드 복수 검색
     * - chip : 필터 칩 단일 선택
     * - 둘 다 없으면 일반 목록 (sort/order 적용)
     * - search와 keywordIds 동시 사용 불가 (서비스에서 가드)
     * - 주의: keywordIds 검색 시 정렬은 고정이라 sort/order는 무시됨
     */
    @GetMapping("")
     public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getPrograms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> keywordNames,
            @RequestParam(required = false) ProgramFilterChip chip,
            @RequestParam(defaultValue = "LATEST") ProgramSortType sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
     ) {
        UUID userId = (userPrincipal != null) ? userPrincipal.getUserId() : null;

        PageResponse<ProgramCardResponse> response = programService.getPrograms(userId, search, keywordNames, chip, sort, order, page, size);
         return ResponseEntity.ok(ApiResponse.success("프로그램 목록 조회에 성공했습니다.", response));
    }

    // 섹션별 목록 조회 : 마감일이 이번달인 목록 중 마감일이 오늘 날짜와 가까운 순으로 정렬
    @GetMapping("/closing-soon")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getProgramsByClosestEndDate(
            @RequestParam(required = false) ProgramFilterChip chip,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramCardResponse> response = programService.getProgramsByClosestEndDate(pageable);
        return ResponseEntity.ok(ApiResponse.success("이번달 안에 끝나는 프로그램 목록 조회에 성공했습니다.", response));
    }

    // 섹션별 목록 조회 : 인기 키워드 프로그램 목록
    // 모아보기 섹션용. chip 적용 가능.
    @GetMapping("/hot-keywords")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getHotKeywordPrograms(
            @RequestParam(required = false) ProgramFilterChip chip,
            @RequestParam(defaultValue = "5") @Min(1) int topN,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramCardResponse> response =
                programService.getProgramsByHotKeywords(topN, chip, pageable);

        return ResponseEntity.ok(ApiResponse.success("인기 키워드 프로그램 목록 조회에 성공했습니다.", response));
    }

    // 섹션별 목록 조회 : 스크랩 많은 순
    // 지금 주목받는 섹션용. chip 없음.
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getHotPrograms(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramCardResponse> response =
                programService.getProgramsByMostScrapped(pageable);

        return ResponseEntity.ok(ApiResponse.success("스크랩 많은 프로그램 목록 조회에 성공했습니다.", response));
    }
    /**
     * 지도
     */

    // 입력 좌표로부터 반경 n km 이내의 프로그램 목록 조회
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getNearbyPrograms(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radiusKm,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramCardResponse> response = programService.getNearbyPrograms(lat, lng, radiusKm, pageable);
        return ResponseEntity.ok(ApiResponse.success("근처 프로그램 목록 조회에 성공했습니다.", response));
    }

    // 지도 뷰포트 바운딩 박스 기반 프로그램 조회 (필터칩 적용, 페이지네이션 없음·최대 200건)
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<List<ProgramCardResponse>>> getProgramsInBounds(
            @Valid @ModelAttribute MapBoundsRequest bounds,
            @RequestParam(required = false) ProgramFilterChip chip
    ) {
        List<ProgramCardResponse> response = programService.getProgramsInBounds(bounds, chip);
        return ResponseEntity.ok(ApiResponse.success("지도 영역 프로그램 목록 조회에 성공했습니다.", response));
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
     * 프로그램 썸네일 이미지 목록 조회
     */
    @GetMapping("/thumbnails")
    public ResponseEntity<ApiResponse<List<ProgramImageResponse>>> getThumbnails() {
        return ResponseEntity.ok(ApiResponse.success("썸네일 목록 조회에 성공했습니다.", programImageService.getThumbnails()));
    }
    /**
     * 프로그램 상태 변경 (관리자)
     */
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PatchMapping("/{program_id}/status")
    public ResponseEntity<ApiResponse<ProgramResponse>> updateProgramStatus(
            @PathVariable("program_id") UUID programId,
            @Valid @RequestBody ProgramStatusUpdateRequest request
    ) {
        ProgramResponse response = programService.updateProgramStatus(programId, request.status());
        return ResponseEntity.ok(ApiResponse.success("프로그램 상태가 변경되었습니다.", response));
    }

}