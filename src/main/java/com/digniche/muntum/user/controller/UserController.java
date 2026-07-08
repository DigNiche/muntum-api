package com.digniche.muntum.user.controller;

import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;

import com.digniche.muntum.user.dto.NicknameUpdateRequest;
import com.digniche.muntum.user.dto.TermsConsentListRequest;
import com.digniche.muntum.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // 닉네임 설정(생성 및 수정)
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("nickname")
    public ResponseEntity<ApiResponse<Void>> setNickname(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid NicknameUpdateRequest request) {
        userService.setNickname(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("닉네임이 설정되었습니다.", null));
    }

    // 사용자 약관 동의
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/terms")
    public ResponseEntity<ApiResponse<Void>> updateTermsConsent(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody @Valid TermsConsentListRequest request) {
        userService.updateTermsConsent(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("약관 동의 상태가 변경되었습니다.", null));
    }

    // 회원 탈퇴
    @PreAuthorize("isAuthenticated()")
    @PostMapping("")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid WithdrawRequest request,
            HttpServletRequest httpRequest) {
        String accessToken = httpRequest.getHeader("Authorization").substring(7);
        userService.withdraw(userPrincipal.getUserId(), request, accessToken);

        return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다.", null));
    }
}
