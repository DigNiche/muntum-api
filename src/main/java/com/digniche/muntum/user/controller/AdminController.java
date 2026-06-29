package com.digniche.muntum.user.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.service.KeywordService;
import com.digniche.muntum.keyword.dto.RegisterKeywordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PreAuthorize("hasAnyRole('CURATOR', 'MANAGER')")
    @PostMapping("/admin/keyword")
    public ResponseEntity<ApiResponse<KeywordResponse>> createKeyword(
            @Valid @RequestBody RegisterKeywordRequest request) {
        KeywordResponse response = keywordService.createKeyword(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("새로운 키워드가 등록되었습니다.", response));
    }

}
