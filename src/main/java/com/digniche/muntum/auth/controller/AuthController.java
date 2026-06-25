package com.digniche.muntum.auth.controller;

import com.digniche.muntum.auth.dto.request.RefreshTokenReissueRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.dto.request.LoginRequest;
import com.digniche.muntum.auth.dto.request.SignUpRequest;
import com.digniche.muntum.auth.dto.response.SignupResponse;
import com.digniche.muntum.auth.service.AuthService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증/인가 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    // Refresh Token 재발행
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(@RequestBody @Valid RefreshTokenReissueRequest request) {
        AuthenticationResponse res = authService.reissueRefreshToken(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("토큰이 재발급되었습니다.", res));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        authService.logout(userPrincipal.getUserId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("로그아웃이 완료되었습니다.", null));
    }
}
