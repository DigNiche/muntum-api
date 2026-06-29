package com.digniche.muntum.user.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.keyword.dto.KeywordActiveRequest;
import com.digniche.muntum.keyword.dto.KeywordActiveResponse;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.service.KeywordService;
import com.digniche.muntum.keyword.dto.KeywordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 관리자
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final KeywordService keywordService;

    /**
     * Taste Keyword
     */
    // 키워드 등록
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PostMapping("/taste/keyword")
    public ResponseEntity<ApiResponse<KeywordResponse>> registerKeyword(
            @Valid @RequestBody KeywordRequest request) {
        KeywordResponse response = keywordService.createKeyword(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("새로운 키워드가 등록되었습니다.", response));
    }

    // 키워드 수정
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PutMapping("/taste/keyword/{keyword_id}")
    public ResponseEntity<ApiResponse<KeywordResponse>> modifyKeyword(
            @PathVariable("keyword_id") UUID keywordId,
            @Valid @RequestBody KeywordRequest request) {
        KeywordResponse response = keywordService.updateKeyword(keywordId, request);
        return ResponseEntity.ok(ApiResponse.success("키워드가 수정되었습니다.", response));
    }

    // 키워드 활성화 / 비활성화
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PatchMapping("/taste/keyword/{keyword_id}/status")
    public ResponseEntity<ApiResponse<KeywordActiveResponse>> changeKeywordStatus(
            @PathVariable("keyword_id") UUID keywordId,
            @Valid @RequestBody KeywordActiveRequest request) {
        KeywordActiveResponse response = keywordService.updateKeywordStatus(keywordId, request);
        String message = request.active() ? "키워드가 활성화되었습니다." : "키워드가 비활성화되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // 키워드 삭제
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @DeleteMapping("/taste/keyword/{keyword_id}")
    public ResponseEntity<ApiResponse<KeywordActiveResponse>> removeKeyword(
            @PathVariable("keyword_id") UUID keywordId) {
        keywordService.deleteKeyword(keywordId);

        return ResponseEntity.ok(ApiResponse.success("키워드가 삭제되었습니다.", null));
    }



}
