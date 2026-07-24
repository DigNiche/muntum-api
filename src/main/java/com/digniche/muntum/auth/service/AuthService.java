package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.RefreshTokenReissueRequest;
import com.digniche.muntum.auth.dto.request.WithdrawRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.dto.request.LoginRequest;
import com.digniche.muntum.auth.dto.request.SignUpRequest;
import com.digniche.muntum.auth.dto.response.SignupResponse;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.global.security.jwt.JwtProvider;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.TermsRepository;
import com.digniche.muntum.user.repository.UserRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;
import com.digniche.muntum.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.global.analytics.event.SignupCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 인증/인가 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final TermsRepository termsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    // 회원가입
    @Transactional
    public SignupResponse signup(SignUpRequest request) {

        String encodedPassword = passwordEncoder.encode(request.password());

        // 신규 가입자 이메일 중복 방지
        if (userRepository.existsByEmailAndStatusNot(request.email(), UserStatus.DELETED)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userRepository.save(request.toEntity(encodedPassword));

        // 사용자 약관 동의 (필수)
        LocalDateTime agreedAt = user.getCreatedAt();
        // -- 여기부터 : 약관 버전 리팩토링 후 삭제
        Terms activeTermsOfService = termsRepository.findByTypeAndActiveTrueAndDeletedAtIsNull(UserTermsType.TERMS_OF_SERVICE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVE_TERMS_NOT_FOUND));
        Terms activePrivcyPolicy = termsRepository.findByTypeAndActiveTrueAndDeletedAtIsNull(UserTermsType.PRIVACY_POLICY)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVE_TERMS_NOT_FOUND));
        // -- 여기까지
        UserTermsAgreement temrs = UserTermsAgreement.builder()
                .user(user)
                .termsOfServiceAt(agreedAt)
                .privacyPolicyAt(agreedAt)
                .version(activeTermsOfService.getVersion()) //request.userTermsAgreementVersion())
                .build();
        userTermsAgreementRepository.save(temrs);

        eventPublisher.publishEvent(new SignupCompletedEvent(user.getId()));

        return new SignupResponse(user.getId(), user.getEmail(), user.getCreatedAt());
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


    // 로그아웃
    public void logout(UUID userId) {
        refreshTokenService.delete(userId);
    }


    // Refresh 토큰 재발급
    @Transactional
    public AuthenticationResponse reissueRefreshToken(RefreshTokenReissueRequest request) {
        log.debug("토큰 재발급 시도");

        String requestToken = request.refreshToken();

        // 1. Refresh Token 서명/만료/Refresh 타입 검증
        Claims claims = jwtProvider.validRefreshToken(requestToken);
        // 2. Claims 사용자 정보 추출
        UUID userId = UUID.fromString(claims.getSubject());

        // 3. Redis 조회
        String storedToken = refreshTokenService.get(userId);
        if (storedToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        // 4. 일치 여부 확인
        if (!storedToken.equals(requestToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. DB에서 최신 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // 6. 기존 토큰 삭제 (Token Rotation)
        refreshTokenService.delete(userId);

        // 7. 새 토큰 발급 및 저장
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);
        refreshTokenService.save(userId, newRefreshToken, jwtProvider.getRefreshTokenExpirationTime());

        return AuthenticationResponse.of(
                newAccessToken, jwtProvider.getAccessTokenExpirationTime(),
                newRefreshToken, jwtProvider.getRefreshTokenExpirationTime(),
                user.getId(), user.getEmail(), user.getNickname()
        );

    }

    // AccessToken 만료까지 남은 시간 계산
    public long calculateTokenTtl(String accessToken) {
        return jwtProvider.getRemainingMillis(accessToken);
    }


}
