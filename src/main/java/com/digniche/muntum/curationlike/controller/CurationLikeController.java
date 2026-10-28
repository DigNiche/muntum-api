package com.digniche.muntum.curationlike.controller;

import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.curationlike.service.CurationLikeService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 큐레이션 좋아요
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/curations/{curation_id}/like")
public class CurationLikeController {

    private final CurationLikeService curationLikeService;

    /**
     * 큐레이션 좋아요
     */
    @PreAuthorize("hasRole('AUDIENCE')")
    @PutMapping
    public ResponseEntity<ApiResponse<CurationLikeResponse>> clickHeart(@PathVariable("curation_id") UUID curationId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CurationLikeResponse response = curationLikeService.like(curationId, userPrincipal.getUserId(), userPrincipal.getUserRole());

        return ResponseEntity.ok(ApiResponse.success("큐레이션 좋아요에 성공했습니다.", response));
    }


    /**
     * 큐레이션 좋아요 취소
     */
    @PreAuthorize("hasRole('AUDIENCE')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<CurationLikeResponse>> unclickHeart(@PathVariable("curation_id") UUID curationId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CurationLikeResponse response = curationLikeService.cancelLike(curationId, userPrincipal.getUserId(), userPrincipal.getUserRole());

        return ResponseEntity.ok(ApiResponse.success("큐레이션 좋아요 취소에 성공했습니다.", response));
    }


    /**
     * 큐레이션 좋아요 수와 내 상태 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CurationLikeResponse>> getHeart(@PathVariable("curation_id") UUID curationId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UUID userId = (userPrincipal != null) ? userPrincipal.getUserId() : null;
        CurationLikeResponse response = curationLikeService.getLike(curationId, userId);

        return ResponseEntity.ok(ApiResponse.success("큐레이션 좋아요 조회에 성공했습니다.", response));
    }
}
