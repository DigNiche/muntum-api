package com.digniche.muntum.user.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.dto.response.MyCuratorProfileResponse;
import com.digniche.muntum.user.service.CuratorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/curators")
public class CuratorController {

    private final CuratorProfileService curatorProfileService;

    @PreAuthorize("hasRole('CURATOR')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyCuratorProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {

        MyCuratorProfileResponse response =
                curatorProfileService.getMyCuratorProfile(
                        userPrincipal.getUserId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "내 큐레이터 프로필 조회에 성공했습니다.",
                        response
                )
        );
    }
}