package com.digniche.muntum.user.controller;

import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.digniche.muntum.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원 탈퇴
    @PostMapping("")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid WithdrawRequest request) {
        userService.withdraw(userPrincipal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("회원탈퇴가 완료되었습니다.", null));
    }
}
