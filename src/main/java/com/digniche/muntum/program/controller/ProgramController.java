package com.digniche.muntum.program.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.dto.request.ProgramUpdateRequest;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.program.dto.response.ProgramResponse;
import com.digniche.muntum.program.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.program.dto.request.ProgramSortType;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 프로그램 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProgramController {

    private final ProgramService programService;

    /**
     * 프로그램 등록 (관리자)
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/admin/program")
    public ApiResponse<ProgramResponse> createProgram(
            @Valid @RequestBody ProgramCreateRequest request
    ) {
        ProgramResponse response = programService.createProgram(request);
        return ApiResponse.success("프로그램이 등록되었습니다.", response);
    }

    /**
     * 프로그램 목록 조회 (누구나)
     */
    @GetMapping("/programs")
    public ApiResponse<PageResponse<ProgramListResponse>> getPrograms(
            @RequestParam(defaultValue = "LATEST") ProgramSortType sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProgramListResponse> response =
                programService.getPrograms(sort, order, page, size);

        return ApiResponse.success("프로그램 목록 조회에 성공했습니다.", response);
    }

    /**
     * 프로그램 단건 조회 (누구나)
     */
    @GetMapping("/program/{program_id}")
    public ApiResponse<ProgramResponse> getProgram(
            @PathVariable("program_id") UUID programId
    ) {
        ProgramResponse response = programService.getProgram(programId);
        return ApiResponse.success("프로그램 조회에 성공했습니다.", response);
    }

    /**
     * 프로그램 수정 (관리자)
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/program/{program_id}")
    public ApiResponse<ProgramResponse> updateProgram(
            @PathVariable("program_id") UUID programId,
            @Valid @RequestBody ProgramUpdateRequest request
    ) {
        ProgramResponse response = programService.updateProgram(programId, request);
        return ApiResponse.success("프로그램이 수정되었습니다.", response);
    }

    /**
     * 프로그램 삭제 (관리자, Soft Delete)
     */
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/program/{program_id}")
    public ApiResponse<Void> deleteProgram(
            @PathVariable("program_id") UUID programId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        programService.deleteProgram(programId, userPrincipal.getUserId());
        return ApiResponse.success("프로그램이 삭제되었습니다.", null);
    }
}