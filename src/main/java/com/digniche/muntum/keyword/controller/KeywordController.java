package com.digniche.muntum.keyword.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.dto.SelectKeywordsRequest;
import com.digniche.muntum.keyword.dto.SelectedKeywordsResponse;
import com.digniche.muntum.keyword.service.KeywordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 키워드 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/taste")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;

    @PostMapping("/me/keyword")
    public ResponseEntity<ApiResponse<SelectedKeywordsResponse>> selectUserKeyword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody SelectKeywordsRequest request) {
        List<KeywordResponse> res = keywordService.setTasteKeywords(userPrincipal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("취향 설정이 저장되었습니다.", new SelectedKeywordsResponse(res)));

    }
}
