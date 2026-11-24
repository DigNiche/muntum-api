package com.digniche.muntum.program.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.service.ProgramQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로그램 조회 컨트롤러
 * - 목적별(섹션별) 프로그램 조회 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/programs")
public class ProgramQueryController {

    private final ProgramQueryService programQueryService;

    /**
     * 모든 사용자의 취향으로 가장 많이 선택된 키워드를 가진 순으로 프로그램 정렬하여 목록 조회
     * - 키워드를 취향으로 고른 사용자 수 기준으로 키워드에 순위 부여하고, 높은 순위의 키워드를 가진 프로그램일수록 앞에 정렬
     */
    @GetMapping("/most-selected-tastes")
    public ResponseEntity<ApiResponse<PageResponse<ProgramCardResponse>>> getPopularTastePrograms(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramCardResponse> response = programQueryService.getMostSelectedTastePrograms(pageable);
        return ResponseEntity.ok(ApiResponse.success("많은 사용자가 선택한 취향 키워드 순 프로그램 목록 조회에 성공했습니다.", response));
    }
}
