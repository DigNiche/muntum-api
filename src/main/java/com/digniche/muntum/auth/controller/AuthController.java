package com.digniche.muntum.auth.controller;

import com.digniche.muntum.auth.dto.request.PasswordFindRequest;
import com.digniche.muntum.auth.dto.request.PasswordResetRequest;
import com.digniche.muntum.auth.dto.request.RefreshTokenReissueRequest;
import com.digniche.muntum.auth.dto.request.VerifyCodeRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.dto.request.LoginRequest;
import com.digniche.muntum.auth.dto.request.SignUpRequest;
import com.digniche.muntum.auth.dto.response.SignupResponse;
import com.digniche.muntum.auth.dto.response.VerifyCodeResponse;
import com.digniche.muntum.auth.service.AuthService;
import com.digniche.muntum.auth.service.PasswordResetService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증/인가 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignUpRequest request) {
        SignupResponse res = authService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", res));
    }


    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@RequestBody @Valid LoginRequest request) {
        AuthenticationResponse res = authService.login(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("로그인이 완료되었습니다.", res));
    }


    // Refresh Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(@RequestBody @Valid RefreshTokenReissueRequest request) {
        AuthenticationResponse res = authService.reissueRefreshToken(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("토큰이 재발급되었습니다.", res));
    }


    // 로그아웃
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        authService.logout(userPrincipal.getUserId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("로그아웃이 완료되었습니다.", null));
    }


    // 비밀번호 찾기 - 인증번호 발송
    @PostMapping("/password/find")
    public ResponseEntity<ApiResponse<Void>> findPassword(@RequestBody @Valid PasswordFindRequest request) {
        passwordResetService.sendVerificationCode(request.email());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("인증번호가 이메일로 발송되었습니다.", null));
    }


    // 비밀번호 찾기 - 인증번호 확인
    @PostMapping("/password/verify-code")
    public ResponseEntity<ApiResponse<VerifyCodeResponse>> verifyCode(@RequestBody @Valid VerifyCodeRequest request) {
        String resetToken = passwordResetService.verifyCode(request.email(), request.code());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("인증번호가 확인되었습니다.", new VerifyCodeResponse(resetToken)));
    }


    // 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid PasswordResetRequest request) {
        passwordResetService.resetPassword(request.resetToken(), request.newPassword());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("비밀번호가 재설정되었습니다.", null));
    }
}
