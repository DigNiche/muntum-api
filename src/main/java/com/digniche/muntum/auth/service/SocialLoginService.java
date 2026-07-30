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
import com.digniche.muntum.auth.social.AppleTokenClient;
import com.digniche.muntum.auth.social.AppleTokenResponse;
import com.digniche.muntum.auth.social.SocialTokenCipher;
import com.digniche.muntum.user.entity.SocialProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    private final AppleTokenClient appleTokenClient;
    private final SocialTokenCipher socialTokenCipher;
    private final PlatformTransactionManager transactionManager;

    public AuthenticationResponse login(
            SocialLoginRequest request
    ) {
        /*
         * 요청 provider에 맞는 검증기 선택
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
         * 앱이 전달한 Identity Token 검증
         */
        SocialUserInfo socialUser = verifier.verify(request);

        /*
         * Apple authorizationCode 교환 및
         * refresh token 암호화
         *
         * 외부 Apple 서버 호출이므로
         * DB 트랜잭션 시작 전에 실행
         */
        String encryptedProviderRefreshToken =
                prepareProviderRefreshToken(
                        request,
                        verifier,
                        socialUser
                );

        /*
         * 아래 DB 작업만 트랜잭션으로 실행
         */
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        AuthenticationResponse response =
                transactionTemplate.execute(status ->
                        completeLogin(
                                socialUser,
                                encryptedProviderRefreshToken
                        )
                );

        if (response == null) {
            throw new BusinessException(
                    ErrorCode.SERVER_ERROR
            );
        }

        return response;
    }

    private String prepareProviderRefreshToken(
            SocialLoginRequest request,
            SocialTokenVerifier verifier,
            SocialUserInfo socialUser
    ) {
        /*
         * 카카오는 추후 별도 처리
         */
        if (request.provider() != SocialProvider.APPLE) {
            return null;
        }

        /*
         * authorizationCode를 Apple 서버에 보내
         * access token, refresh token, id token 수신
         */
        AppleTokenResponse tokenResponse =
                appleTokenClient.exchangeAuthorizationCode(
                        request.authorizationCode()
                );

        if (tokenResponse.idToken() == null
                || tokenResponse.idToken().isBlank()
                || tokenResponse.refreshToken() == null
                || tokenResponse.refreshToken().isBlank()) {
            throw new BusinessException(
                    ErrorCode.APPLE_TOKEN_EXCHANGE_FAILED
            );
        }

        /*
         * authorizationCode 교환 결과의 id_token도 검증
         *
         * 앱이 보낸 Identity Token의 사용자와
         * authorizationCode의 사용자가 동일한지 확인
         */
        SocialLoginRequest exchangedTokenRequest =
                new SocialLoginRequest(
                        SocialProvider.APPLE,
                        tokenResponse.idToken(),
                        null,
                        request.nonce()
                );

        SocialUserInfo exchangedUser =
                verifier.verify(exchangedTokenRequest);
        // 프론트엔드가 줬던 신분증의 유저(socialUser) == 애플이 방금 준 신분증의 유저(exchangedUser) 비교
        if (!socialUser.providerUserId().equals(
                exchangedUser.providerUserId()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }

        /*
         * DB에는 refresh token 평문이 아닌
         * AES-GCM 암호문 저장
         */
        return socialTokenCipher.encrypt(
                tokenResponse.refreshToken()
        );
    }

    private AuthenticationResponse completeLogin(
            SocialUserInfo socialUser,
            String encryptedProviderRefreshToken
    ) {
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

        /*
         * 신규·기존 Apple 회원 모두
         * 가장 최근에 발급받은 refresh token으로 갱신
         */
        if (encryptedProviderRefreshToken != null) {
            socialAccount.updateProviderRefreshToken(
                    encryptedProviderRefreshToken
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