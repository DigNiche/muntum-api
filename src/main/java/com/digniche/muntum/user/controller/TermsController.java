package com.digniche.muntum.user.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.user.dto.request.TermsCreateRequest;
import com.digniche.muntum.user.dto.request.TermsUpdateRequest;
import com.digniche.muntum.user.dto.response.TermsResponse;
import com.digniche.muntum.user.dto.response.TermsSummaryResponse;
import com.digniche.muntum.user.service.TermsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 약관 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermsController {

    private final TermsService termsService;

    // 약관 등록
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PostMapping("")
    public ResponseEntity<ApiResponse<TermsResponse>> registerTerms(
            @Valid @RequestBody TermsCreateRequest request
    ) {
        TermsResponse response = termsService.createTerms(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("약관이 등록되었습니다.", response));
    }

    // 약관 수정
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PutMapping("/{terms_id}")
    public ResponseEntity<ApiResponse<TermsResponse>> rewriteTerms(
            @PathVariable("terms_id") UUID termsId,
            @Valid @RequestBody TermsUpdateRequest request
    ) {
        TermsResponse response = termsService.updateTerms(termsId, request);
        return ResponseEntity.ok(ApiResponse.success("약관이 수정되었습니다.", response));
    }

    // 약관 활성화/게시 — 같은 타입의 기존 게시 버전은 자동 비활성화
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PatchMapping("/{terms_id}/activate")
    public ResponseEntity<ApiResponse<TermsResponse>> activateTerms(
            @PathVariable("terms_id") UUID termsId
    ) {
        TermsResponse response = termsService.activateTerms(termsId);
        return ResponseEntity.ok(ApiResponse.success("약관이 게시되었습니다.", response));
    }

    // 약관 삭제
    @PreAuthorize("hasAnyRole('MANAGER')")
    @DeleteMapping("/{terms_id}")
    public ResponseEntity<ApiResponse<Void>> deleteTerms(
            @PathVariable("terms_id") UUID termsId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        termsService.deleteTerms(termsId, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("약관이 삭제되었습니다.", null));
    }

    // 약관 목록 조회
    @PreAuthorize("hasAnyRole('MANAGER')")
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<TermsSummaryResponse>>> getActiveTerms() {
        return ResponseEntity.ok(
                ApiResponse.success("약관 목록 조회에 성공했습니다.", termsService.getActiveTerms()));
    }

    // 약관 단건 조회 - 본문 포함 (로그인 사용자)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{terms_id}")
    public ResponseEntity<ApiResponse<TermsResponse>> getTerms(
            @PathVariable("terms_id") UUID termsId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("약관 조회에 성공했습니다.", termsService.getTerms(termsId)));
    }
}
