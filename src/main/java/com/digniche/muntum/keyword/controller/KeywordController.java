package com.digniche.muntum.keyword.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.dto.SelectKeywordsRequest;
import com.digniche.muntum.keyword.dto.SelectedKeywordsResponse;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.service.KeywordService;
import com.digniche.muntum.program.service.ProgramKeywordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 키워드 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/taste")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;
    private final ProgramKeywordService programKeywordService;

    // 키워드 설정
    @PostMapping("/me/keyword")
    public ResponseEntity<ApiResponse<SelectedKeywordsResponse>> selectUserKeyword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody SelectKeywordsRequest request) {
        List<KeywordResponse> res = keywordService.setTasteKeywords(userPrincipal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("취향 설정이 저장되었습니다.", new SelectedKeywordsResponse(res)));

    }
    // 내 취향 키워드 목록 조회
    @GetMapping("/me/keywords")
    public ResponseEntity<ApiResponse<SelectedKeywordsResponse>> getMyKeywords(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<KeywordResponse> keywords = keywordService.retrieveSelectedKeywords(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내 취향 키워드 목록을 조회했습니다.", new SelectedKeywordsResponse(keywords)));
    }

    // 프로그램과 연결된 Keyword 목록 조회
    @GetMapping("/keywords")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getProgramsKeywords() {
        List<Keyword> keywords = programKeywordService.retrieveProgramsKeywords();
        List<KeywordResponse> res = keywords.stream().map(KeywordResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success("모든 프로그램의 키워드 목록을 조회했습니다.", res));
    }


 /*   // 인기 키워드 목록 정렬
    @GetMapping("/keywords/hot")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getPopularKeywords(
            @RequestParam(defaultValue = "6") @Min(1) int topN
    ) {
        List<KeywordResponse> keywords = keywordService.getHotKeywords(topN);
        return ResponseEntity.ok(ApiResponse.success("인기 키워드 목록을 조회했습니다.", keywords));
    }*/
}
