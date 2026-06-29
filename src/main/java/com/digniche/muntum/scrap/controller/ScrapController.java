package com.digniche.muntum.scrap.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.scrap.service.ScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 스크랩 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ScrapController {

    private final ScrapService scrapService;

    /**
     * 스크랩 등록 (로그인 사용자)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/program/{program_id}/scrap")
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
    @DeleteMapping("/program/{program_id}/scrap")
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
    @GetMapping("/scraps")
    public ApiResponse<PageResponse<ProgramListResponse>> getMyScraps(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ProgramListResponse> response =
                scrapService.getMyScraps(userPrincipal.getUserId(), pageable);

        return ApiResponse.success("스크랩 목록 조회에 성공했습니다.", response);
    }
}