package com.digniche.muntum.program.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.dto.response.ProgramResponse;
import com.digniche.muntum.program.service.ProgramService;
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
@RequestMapping("/api/v1/manager/programs")
public class ManagerProgramController {

    private final ProgramService programService;

    /**
     * 관리자 프로그램 목록·검색
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<ProgramCardResponse>>>
    getPrograms(
            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size
    ) {
        PageResponse<ProgramCardResponse> response =
                programService.getProgramsForManager(search, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 프로그램 목록 조회에 성공했습니다.", response)
        );
    }

    /**
     * 관리자 프로그램 상세 조회
     */
    @GetMapping("/{program_id}")
    public ResponseEntity<ApiResponse<ProgramResponse>>
    getProgram(
            @PathVariable("program_id")
            UUID programId
    ) {
        ProgramResponse response = programService.getProgramForManager(programId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 프로그램 상세 조회에 성공했습니다.", response)
        );
    }
}