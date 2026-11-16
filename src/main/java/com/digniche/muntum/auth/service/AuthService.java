package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.RefreshTokenReissueRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.dto.request.LoginRequest;
import com.digniche.muntum.auth.dto.request.SignUpRequest;
import com.digniche.muntum.auth.dto.response.SignupResponse;
import com.digniche.muntum.auth.dto.response.TokenResponse;
import com.digniche.muntum.global.redis.EmailVerificationRedisService;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.global.security.jwt.JwtProvider;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.global.analytics.event.SignupCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;

/**
 * 인증/인가 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SignupConsentRecorder signupConsentRecorder;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenIssuer tokenIssuer;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailVerificationRedisService emailVerificationRedisService;
    private final PlatformTransactionManager transactionManager;

    // 회원가입

    public SignupResponse signup(SignUpRequest request) {

        // 1. 이메일 인증 토큰 검증
        String verifiedEmail =
                emailVerificationRedisService.getEmailBySignupToken(
                        request.signupToken()
                );

        if (verifiedEmail == null
                || !verifiedEmail.equalsIgnoreCase(request.email().trim())) {
            throw new BusinessException(ErrorCode.INVALID_SIGNUP_TOKEN);
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        // 2. 회원 및 약관 동의 저장
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        User committedUser = transactionTemplate.execute(status -> {

            if (userRepository.existsByEmailAndStatusNot(
                    request.email(),
                    UserStatus.DELETED
            )) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }

            User user = userRepository.save(
                    request.toEntity(encodedPassword, UserRole.AUDIENCE)
            );

            user.verifyEmail();

            if (request.signupConsent() != null) {
                signupConsentRecorder.saveExplicitSignup(
                        user,
                        request.signupConsent()
                );
            } else {
                signupConsentRecorder.saveLegacyUnverifiedSignup(user);
            }

            user.updateLastLogin();

            userRepository.flush();

            // 기존 Amplitude 가입 완료 이벤트 유지
            eventPublisher.publishEvent(
                    new SignupCompletedEvent(user.getId())
            );

            return user;
        });

        if (committedUser == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        // 3. DB 커밋 완료 후 가입 토큰 소비
        emailVerificationRedisService.deleteSignupToken(
                request.signupToken()
        );

        // 4. JWT 발급
        TokenResponse token = tokenIssuer.issue(committedUser);

        return new SignupResponse(
                committedUser.getId(),
                committedUser.getEmail(),
                committedUser.getCreatedAt(),
                token
        );
    }




    public AuthenticationResponse login(LoginRequest request) {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        User committedUser = transactionTemplate.execute(status -> {

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() ->
                            new BusinessException(ErrorCode.USER_NOT_FOUND)
                    );

            if (user.getPassword() == null) {
                throw new BusinessException(
                        ErrorCode.PASSWORD_LOGIN_NOT_AVAILABLE
                );
            }

            if (!passwordEncoder.matches(
                    request.password(),
                    user.getPassword()
            )) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }

            if (!user.isActive()) {
                throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
            }

            user.updateLastLogin();

            return user;
        });

        if (committedUser == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        return AuthenticationResponse.of(
                tokenIssuer.issue(committedUser),
                committedUser
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
        return AuthenticationResponse.of(tokenIssuer.issue(user), user);

    }

    // AccessToken 만료까지 남은 시간 계산
    public long calculateTokenTtl(String accessToken) {
        return jwtProvider.getRemainingMillis(accessToken);
    }


}
