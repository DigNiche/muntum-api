package com.digniche.muntum.programreaction.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.programreaction.dto.request.ProgramReactionUpdateRequest;
import com.digniche.muntum.programreaction.dto.response.ProgramReactionUpdateResponse;
import com.digniche.muntum.programreaction.service.ProgramReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.programreaction.entity.ReactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

/**
 * 프로그램 좋아요·싫어요 API 컨트롤러
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/program-reactions")
public class ProgramReactionController {

    private final ProgramReactionService programReactionService;

    /**
     * 프로그램 반응 변경
     *
     * LIKE    : 좋아요로 설정
     * DISLIKE : 싫어요로 설정
     * NONE    : 반응 해제
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{program_id}")
    public ApiResponse<ProgramReactionUpdateResponse> updateReaction(
            @PathVariable("program_id") UUID programId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProgramReactionUpdateRequest request
    ) {
        ProgramReactionUpdateResponse response =
                programReactionService.updateReaction(
                        userPrincipal.getUserId(),
                        programId,
                        request.reactionState()
                );

        return ApiResponse.success(
                "프로그램 반응이 변경되었습니다.",
                response
        );
    }
    /**
     * 내 좋아요·싫어요 프로그램 목록 조회
     *
     * reactionType:
     * - LIKE: 좋아요 목록
     * - DISLIKE: 싫어요 목록
     *
     * 최근 반응 순으로 반환한다.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ApiResponse<PageResponse<ProgramCardResponse>> getMyReactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam ReactionType reactionType,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size
    ) {
        PageResponse<ProgramCardResponse> response =
                programReactionService.getMyReactions(
                        userPrincipal.getUserId(),
                        reactionType,
                        page,
                        size
                );

        return ApiResponse.success(
                "프로그램 반응 목록 조회에 성공했습니다.",
                response
        );
    }
}