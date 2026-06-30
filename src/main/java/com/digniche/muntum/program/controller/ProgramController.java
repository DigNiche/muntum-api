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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PostMapping("/admin/programs")
    public ResponseEntity<ApiResponse<ProgramResponse>> createProgram(
            @Valid @RequestBody ProgramCreateRequest request
    ) {
        ProgramResponse response = programService.createProgram(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로그램이 등록되었습니다.", response));
    }

    /**
     * 프로그램 목록 조회 (누구나)
     */
    @GetMapping("/programs")
    public ResponseEntity<ApiResponse<Page<ProgramListResponse>>> getPrograms(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProgramListResponse> response = programService.getPrograms(pageable);
        return ResponseEntity.ok(ApiResponse.success("프로그램 목록 조회에 성공했습니다.", response));
    }

    /**
     * 프로그램 단건 조회 (누구나)
     */
    @GetMapping("/programs/{program_id}")
    public ResponseEntity<ApiResponse<ProgramResponse>> getProgram(
            @PathVariable("program_id") UUID programId
    ) {
        ProgramResponse response = programService.getProgram(programId);
        return ResponseEntity.ok(ApiResponse.success("프로그램 조회에 성공했습니다.", response));
    }

    /**
     * 프로그램 수정 (관리자)
     */
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PutMapping("/programs/{program_id}")
    public ResponseEntity<ApiResponse<ProgramResponse>> updateProgram(
            @PathVariable("program_id") UUID programId,
            @Valid @RequestBody ProgramUpdateRequest request
    ) {
        ProgramResponse response = programService.updateProgram(programId, request);
        return ResponseEntity.ok(ApiResponse.success("프로그램이 수정되었습니다.", response));
    }

    /**
     * 프로그램 삭제 (관리자, Soft Delete)
     */
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @DeleteMapping("/programs/{program_id}")
    public ResponseEntity<ApiResponse<Void>> deleteProgram(
            @PathVariable("program_id") UUID programId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        programService.deleteProgram(programId, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("프로그램이 삭제되었습니다.", null));
    }
}