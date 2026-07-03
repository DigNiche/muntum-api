package com.digniche.muntum.search.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.service.KeywordService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final KeywordService keywordService;

    /**
     * 인기 키워드 목록 조회
     * 기준: 사용자들이 취향으로 많이 선택한 키워드 상위 N개
     */
    @GetMapping("/top_keyword")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getPopularKeywords(
            @RequestParam(defaultValue = "6") @Min(1) int topN
    ) {
        List<KeywordResponse> keywords = keywordService.getHotKeywords(topN);
        return ResponseEntity.ok(ApiResponse.success("인기 키워드 목록을 조회했습니다.", keywords));
    }


}