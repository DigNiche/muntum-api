package com.digniche.muntum.keyword.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.dto.SelectKeywordsRequest;
import com.digniche.muntum.keyword.dto.SelectedKeywordsResponse;
import com.digniche.muntum.keyword.service.KeywordService;
import com.digniche.muntum.keyword.service.TasteService;
import com.digniche.muntum.program.dto.request.ProgramFilterChip;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 내 취향 프로그램 목록 컨트롤러
 * - URL은 taste 도메인이지만 반환은 프로그램이라 로직은 ProgramService에 위임
 */
@RestController
@RequestMapping("/api/v1/taste")
@RequiredArgsConstructor
public class TasteController {

    private final ProgramService programService;
    private final TasteService tasteService;

    // 취향 설정
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/keywords")
    public ResponseEntity<ApiResponse<SelectedKeywordsResponse>> selectUserKeyword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody SelectKeywordsRequest request) {
        List<KeywordResponse> res = tasteService.setTasteKeywords(userPrincipal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("취향 설정이 저장되었습니다.", new SelectedKeywordsResponse(res)));

    }
    // 내 취향 키워드 목록 조회
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/keywords")
    public ResponseEntity<ApiResponse<SelectedKeywordsResponse>> getMyKeywords(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<KeywordResponse> keywords = tasteService.retrieveSelectedKeywords(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내 취향 키워드 목록을 조회했습니다.", new SelectedKeywordsResponse(keywords)));
    }

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