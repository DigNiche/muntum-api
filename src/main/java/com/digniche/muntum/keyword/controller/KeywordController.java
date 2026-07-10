package com.digniche.muntum.keyword.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.keyword.dto.*;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.service.KeywordService;
import com.digniche.muntum.program.service.ProgramKeywordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

/**
 * 키워드 컨트롤러
 */
@RestController
@Validated
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final ProgramKeywordService programKeywordService;
    private final KeywordService keywordService;

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
    @PutMapping("/{keyword_id}")
    public ResponseEntity<ApiResponse<KeywordResponse>> modifyKeyword(
            @PathVariable("keyword_id") UUID keywordId,
            @Valid @RequestBody KeywordRequest request) {
        KeywordResponse response = keywordService.updateKeyword(keywordId, request);
        return ResponseEntity.ok(ApiResponse.success("키워드가 수정되었습니다.", response));
    }

    // 키워드 삭제
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @DeleteMapping("/{keyword_id}")
    public ResponseEntity<ApiResponse<Void>> removeKeyword(
            @PathVariable("keyword_id") UUID keywordId) {
        keywordService.deleteKeyword(keywordId);

        return ResponseEntity.ok(ApiResponse.success("키워드가 삭제되었습니다.", null));
    }

    // 키워드 활성화 / 비활성화
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PatchMapping("/{keyword_id}/status")
    public ResponseEntity<ApiResponse<KeywordActiveResponse>> changeKeywordStatus(
            @PathVariable("keyword_id") UUID keywordId,
            @Valid @RequestBody KeywordActiveRequest request) {
        KeywordActiveResponse response = keywordService.updateKeywordStatus(keywordId, request);
        String message = request.active() ? "키워드가 활성화되었습니다." : "키워드가 비활성화되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // 키워드 목록 조회
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @GetMapping("")
    public ResponseEntity<ApiResponse<Page<KeywordResponse>>> getKeywords(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<KeywordResponse> response = keywordService.retrieveKeywords(pageable);
        return ResponseEntity.ok(ApiResponse.success("키워드 목록 조회에 성공했습니다.", response));
    }

    // 프로그램과 연결된 Keyword 목록 조회
    @GetMapping("/tagged")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getProgramsKeywords() {
        List<Keyword> keywords = programKeywordService.retrieveProgramsKeywords();
        List<KeywordResponse> res = keywords.stream().map(KeywordResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success("모든 프로그램의 키워드 목록을 조회했습니다.", res));
    }

    // 키워드 단건 조회
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @GetMapping("/{keyword_id}")
    public ResponseEntity<ApiResponse<KeywordResponse>> getKeyword(
            @PathVariable("keyword_id") UUID keywordId) {
        KeywordResponse response = keywordService.retrieveKeyword(keywordId);
        return ResponseEntity.ok(ApiResponse.success("키워드 조회에 성공했습니다.", response));
    }

    // 인기 키워드 목록 정렬
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getPopularKeywords(
            @RequestParam(defaultValue = "6") @Min(value = 1, message = "topN은 1 이상이어야 합니다") int topN
    ) {
        List<KeywordResponse> keywords = keywordService.getTopKeywords(topN);
        return ResponseEntity.ok(ApiResponse.success("인기 키워드 목록을 조회했습니다.", keywords));
    }
}
