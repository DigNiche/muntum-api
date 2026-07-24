package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.social.SocialTokenVerifier;
import com.digniche.muntum.auth.social.SocialUserInfo;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.global.redis.RefreshTokenService;
import com.digniche.muntum.global.security.jwt.JwtProvider;
import com.digniche.muntum.user.entity.SocialAccount;
import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.user.entity.UserTermsAgreement;
import com.digniche.muntum.user.entity.UserTermsType;
import com.digniche.muntum.user.repository.SocialAccountRepository;
import com.digniche.muntum.user.repository.TermsRepository;
import com.digniche.muntum.user.repository.UserRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final List<SocialTokenVerifier> verifiers;

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final TermsRepository termsRepository;

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthenticationResponse login(
            SocialLoginRequest request
    ) {
        /*
         * APPLE 요청이면 AppleTokenVerifier 선택
         * KAKAO 요청이면 추후 KakaoTokenVerifier 선택
         */
        SocialTokenVerifier verifier = verifiers.stream()
                .filter(v -> v.supports() == request.provider())
                .findFirst()
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER
                        )
                );

        /*
         * Apple Identity Token 검증
         */
        SocialUserInfo socialUser = verifier.verify(request);

        /*
         * 기존 소셜 계정이면 조회,
         * 최초 로그인이라면 신규 User와 SocialAccount 생성
         */
        SocialAccount socialAccount =
                socialAccountRepository
                        .findByProviderAndProviderUserId(
                                socialUser.provider(),
                                socialUser.providerUserId()
                        )
                        .orElseGet(() ->
                                createSocialAccount(socialUser)
                        );

        User user = socialAccount.getUser();

        if (!user.isActive()) {
            throw new BusinessException(
                    ErrorCode.INACTIVE_ACCOUNT
            );
        }

        user.updateLastLogin();

        String accessToken =
                jwtProvider.generateAccessToken(user);

        String refreshToken =
                jwtProvider.generateRefreshToken(user);

        refreshTokenService.save(
                user.getId(),
                refreshToken,
                jwtProvider.getRefreshTokenExpirationTime()
        );

        return AuthenticationResponse.of(
                accessToken,
                jwtProvider.getAccessTokenExpirationTime(),
                refreshToken,
                jwtProvider.getRefreshTokenExpirationTime(),
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }

    /**
     * Apple 최초 로그인 회원 생성
     */
    private SocialAccount createSocialAccount(
            SocialUserInfo socialUser
    ) {
        String email = socialUser.email();

        /*
         * 문틈 정책:
         * 소셜 신규 가입도 이메일 필수
         */
        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_EMAIL_REQUIRED
            );
        }

        /*
         * 계정 자동 연결 기능을 사용하지 않으므로,
         * 같은 이메일의 일반·다른 소셜 계정이 있으면 가입 거절
         */
        if (userRepository.existsByEmailAndStatusNot(
                email,
                UserStatus.DELETED
        )) {
            throw new BusinessException(
                    ErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        User user = User.createSocialUser(
                email,
                socialUser.emailVerified()
        );

        userRepository.save(user);

        saveRequiredTermsAgreement(user);

        SocialAccount socialAccount =
                SocialAccount.builder()
                        .user(user)
                        .provider(socialUser.provider())
                        .providerUserId(
                                socialUser.providerUserId()
                        )
                        .providerEmail(email)
                        .build();

        return socialAccountRepository.save(
                socialAccount
        );
    }

    /**
     * 기존 일반 회원가입과 동일하게
     * 현재 활성 필수 약관 동의 내역 생성
     */
    private void saveRequiredTermsAgreement(User user) {
        Terms activeTermsOfService =
                termsRepository
                        .findByTypeAndActiveTrueAndDeletedAtIsNull(
                                UserTermsType.TERMS_OF_SERVICE
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.ACTIVE_TERMS_NOT_FOUND
                                )
                        );

        /*
         * 개인정보 처리방침도 활성 상태인지 확인
         */
        termsRepository
                .findByTypeAndActiveTrueAndDeletedAtIsNull(
                        UserTermsType.PRIVACY_POLICY
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ACTIVE_TERMS_NOT_FOUND
                        )
                );

        LocalDateTime agreedAt = LocalDateTime.now();

        UserTermsAgreement agreement =
                UserTermsAgreement.builder()
                        .user(user)
                        .termsOfServiceAt(agreedAt)
                        .privacyPolicyAt(agreedAt)
                        .version(
                                activeTermsOfService.getVersion()
                        )
                        .build();

        userTermsAgreementRepository.save(agreement);
    }
}