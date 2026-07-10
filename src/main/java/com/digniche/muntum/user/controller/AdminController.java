package com.digniche.muntum.user.controller;

import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.user.dto.UserProfileResponse;
import com.digniche.muntum.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    // 사용자 관리 - 사용자 목록 조회 (닉네임 또는 이메일 검색)
    @PreAuthorize("hasAnyRole('MANAGER')")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<UserProfileResponse> response = userService.getUsers(search, page, size);
        return ResponseEntity.ok(ApiResponse.success("사용자 목록 조회에 성공했습니다.", response));
    }
}