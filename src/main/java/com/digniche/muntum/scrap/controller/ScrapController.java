package com.digniche.muntum.scrap.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.scrap.service.ScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.digniche.muntum.scrap.dto.request.ScrapSortType;
import org.springframework.data.domain.Sort;

import java.util.UUID;

/**
 * 스크랩 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scraps")
public class ScrapController {

    private final ScrapService scrapService;

    /**
     * 스크랩 등록 (로그인 사용자)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{program_id}")
    public ApiResponse<Void> createScrap(
            @PathVariable("program_id") UUID programId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        scrapService.createScrap(userPrincipal.getUserId(), programId);
        return ApiResponse.success("스크랩이 등록되었습니다.", null);
    }

    /**
     * 스크랩 해제 (로그인 사용자)
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("{program_id}")
    public ApiResponse<Void> deleteScrap(
            @PathVariable("program_id") UUID programId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        scrapService.deleteScrap(userPrincipal.getUserId(), programId);
        return ApiResponse.success("스크랩이 해제되었습니다.", null);
    }

    /**
     * 내 스크랩 목록 조회 (로그인 사용자)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ApiResponse<PageResponse<ProgramListResponse>> getMyScraps(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "SCRAPPED_AT") ScrapSortType sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProgramListResponse> response =
                scrapService.getMyScraps(
                        userPrincipal.getUserId(),
                        sort,
                        order,
                        page,
                        size
                );

        return ApiResponse.success("스크랩 목록 조회에 성공했습니다.", response);
    }
}