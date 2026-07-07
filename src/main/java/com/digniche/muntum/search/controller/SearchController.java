package com.digniche.muntum.search.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.service.KeywordService;
import com.digniche.muntum.search.service.RecentSearchService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final RecentSearchService recentSearchService;

    // 최근 검색어 목록 조회 (로그인 유저, 최근순 10개)
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<String>>> getRecentSearches(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<String> recent = recentSearchService.getRecent(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("최근 검색어를 조회했습니다.", recent));
    }

    // 최근 검색어 개별 삭제
    @DeleteMapping("/recent")
    public ResponseEntity<ApiResponse<Void>> deleteRecentSearch(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String query
    ) {
        recentSearchService.delete(userPrincipal.getUserId(), query);
        return ResponseEntity.ok(ApiResponse.success("검색어를 삭제했습니다.", null));
    }

    // 최근 검색어 전체 삭제
    @DeleteMapping("/recent/all")
    public ResponseEntity<ApiResponse<Void>> deleteAllRecentSearches(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        recentSearchService.deleteAll(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("최근 검색어를 전체 삭제했습니다.", null));
    }
}