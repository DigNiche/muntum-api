package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.dto.request.LoginRequest;
import com.digniche.muntum.auth.dto.request.SignUpRequest;
import com.digniche.muntum.auth.dto.response.SignupResponse;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.global.security.jwt.JwtProvider;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증/인가 서비스
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    // 회원가입
    @Transactional
    public SignupResponse signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userRepository.save(request.toEntity(encodedPassword));

        // TODO: 이메일 중복 여부 확인

        return SignupResponse.of(user.getId(), user.getEmail(), user.getCreatedAt());
    }

    // 로그인
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }

        user.updateLastLogin();

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        refreshTokenService.save(user.getId(), refreshToken, jwtProvider.getRefreshTokenExpirationTime());

        return AuthenticationResponse.of(
                accessToken, jwtProvider.getAccessTokenExpirationTime(),
                refreshToken, jwtProvider.getRefreshTokenExpirationTime(),
                user.getId(), user.getEmail(), user.getNickname()
        );
    }

    // Refresh 토큰 재발급

}
