package com.digniche.muntum.keyword.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.program.dto.request.ProgramFilterChip;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 내 취향 프로그램 목록 컨트롤러
 * - URL은 taste 도메인이지만 반환은 프로그램이라 로직은 ProgramService에 위임
 */
@RestController
@RequestMapping("/api/v1/taste")
@RequiredArgsConstructor
public class TasteController {

    private final ProgramService programService;

    // 내 취향 프로그램 목록 조회 (로그인 사용자)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getTastePrograms(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) ProgramFilterChip chip,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProgramCardResponse> response =
                programService.getTastePrograms(userPrincipal.getUserId(), chip, page, size);
        return ResponseEntity.ok(ApiResponse.success("내 취향 프로그램 목록 조회에 성공했습니다.", response));
    }
}