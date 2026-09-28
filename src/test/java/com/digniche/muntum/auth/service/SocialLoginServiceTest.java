package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.auth.dto.response.AuthenticationResponse;
import com.digniche.muntum.auth.dto.response.TokenResponse;
import com.digniche.muntum.auth.social.AppleTokenClient;
import com.digniche.muntum.auth.social.AppleTokenResponse;
import com.digniche.muntum.auth.social.AppleTokenVerifier;
import com.digniche.muntum.auth.social.SocialTokenCipher;
import com.digniche.muntum.auth.social.SocialUserInfo;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.SocialAccount;
import com.digniche.muntum.user.entity.SocialProvider;
import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.user.entity.UserTermsType;
import com.digniche.muntum.user.repository.SocialAccountRepository;
import com.digniche.muntum.user.repository.TermsRepository;
import com.digniche.muntum.user.repository.UserRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 소셜 연동 로그인의 토큰 발급 대상 검증
 * - Apple 검증·토큰 교환·DB·토큰 발급은 mock, completeLogin 분기는 실제 실행
 */
@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    private static final String APPLE_SUB = "apple-sub-123";
    private static final String EMAIL = "apple@example.com";
    private static final String AUTHORIZATION_CODE = "auth-code";
    private static final String APPLE_REFRESH_TOKEN = "apple-refresh-token";
    private static final String ENCRYPTED_APPLE_REFRESH_TOKEN = "encrypted-apple-refresh-token";

    private static final SocialLoginRequest REQUEST = new SocialLoginRequest(SocialProvider.APPLE, "identity-token", AUTHORIZATION_CODE, "nonce");
    private static final SocialUserInfo APPLE_USER = new SocialUserInfo(SocialProvider.APPLE, APPLE_SUB, EMAIL, true);
    private static final TokenResponse ISSUED_TOKEN = TokenResponse.of("issued-access-token", 3_600_000L, "issued-refresh-token", 1_209_600_000L);

    @Mock AppleTokenVerifier appleTokenVerifier;
    @Mock SocialAccountRepository socialAccountRepository;
    @Mock UserRepository userRepository;
    @Mock UserTermsAgreementRepository userTermsAgreementRepository;
    @Mock TermsRepository termsRepository;
    @Mock TokenIssuer tokenIssuer;
    @Mock AppleTokenClient appleTokenClient;
    @Mock SocialTokenCipher socialTokenCipher;
    @Mock PlatformTransactionManager transactionManager;

    private SocialLoginService socialLoginService;


    @BeforeEach
    void setUp() {
        socialLoginService = new SocialLoginService(
                List.of(appleTokenVerifier),
                socialAccountRepository, userRepository, userTermsAgreementRepository, termsRepository,
                tokenIssuer,
                appleTokenClient,
                socialTokenCipher,
                transactionManager
        );

        when(appleTokenVerifier.supports()).thenReturn(SocialProvider.APPLE);
    }


    @Test
    void 기존_활성_회원은_조회된_User로_토큰을_발급하고_응답에_담는다() {
        User existingUser = createUser();
        existingUser.updateNickname("기존닉네임");
        SocialAccount socialAccount = createSocialAccount(existingUser);

        givenAppleVerifiedAndExchanged();
        when(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, APPLE_SUB))
                .thenReturn(Optional.of(socialAccount));
        when(tokenIssuer.issue(any(User.class))).thenReturn(ISSUED_TOKEN);

        AuthenticationResponse response = socialLoginService.login(REQUEST);

        // 토큰 발급 대상이 조회된 바로 그 User인지
        ArgumentCaptor<User> issuedFor = ArgumentCaptor.forClass(User.class);
        verify(tokenIssuer).issue(issuedFor.capture());
        assertThat(issuedFor.getValue()).isSameAs(existingUser);

        // 발급된 토큰과 User 정보가 응답에 그대로 담기는지
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo(ISSUED_TOKEN.accessToken());
        assertThat(response.accessExpiresIn()).isEqualTo(ISSUED_TOKEN.accessExpiresIn());
        assertThat(response.refreshToken()).isEqualTo(ISSUED_TOKEN.refreshToken());
        assertThat(response.refreshExpiresIn()).isEqualTo(ISSUED_TOKEN.refreshExpiresIn());
        assertThat(response.userId()).isEqualTo(existingUser.getId());
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.nickname()).isEqualTo("기존닉네임");

        // 기존 동작 유지: 로그인 시각, Apple refresh token 갱신
        assertThat(existingUser.getLastLoginAt()).isNotNull();
        assertThat(socialAccount.getProviderRefreshToken()).isEqualTo(ENCRYPTED_APPLE_REFRESH_TOKEN);

        // 기존 회원이므로 새로 생성하지 않는다
        verify(userRepository, never()).save(any());
        verify(socialAccountRepository, never()).save(any());
    }


    @Test
    void 신규_Apple_회원은_생성된_User로_토큰을_발급한다() {
        givenAppleVerifiedAndExchanged();
        when(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, APPLE_SUB))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmailAndStatusNot(EMAIL, UserStatus.DELETED)).thenReturn(false);
        givenActiveTerms();
        when(socialAccountRepository.save(any(SocialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenIssuer.issue(any(User.class))).thenReturn(ISSUED_TOKEN);

        AuthenticationResponse response = socialLoginService.login(REQUEST);

        // 생성·저장된 User와 SocialAccount
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        ArgumentCaptor<SocialAccount> savedAccount = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(savedAccount.capture());
        verify(userTermsAgreementRepository).save(any());

        assertThat(savedUser.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getValue().getRole()).isEqualTo(UserRole.AUDIENCE);
        assertThat(savedUser.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedAccount.getValue().getUser()).isSameAs(savedUser.getValue());
        assertThat(savedAccount.getValue().getProviderUserId()).isEqualTo(APPLE_SUB);

        // 토큰 발급 대상이 방금 저장한 User인지
        ArgumentCaptor<User> issuedFor = ArgumentCaptor.forClass(User.class);
        verify(tokenIssuer).issue(issuedFor.capture());
        assertThat(issuedFor.getValue()).isSameAs(savedUser.getValue());

        // 신규 회원 응답: 토큰 포함, 닉네임 미설정
        assertThat(response.accessToken()).isEqualTo(ISSUED_TOKEN.accessToken());
        assertThat(response.refreshToken()).isEqualTo(ISSUED_TOKEN.refreshToken());
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.nickname()).isNull();

        assertThat(savedUser.getValue().getLastLoginAt()).isNotNull();
        assertThat(savedAccount.getValue().getProviderRefreshToken()).isEqualTo(ENCRYPTED_APPLE_REFRESH_TOKEN);
    }


    @Test
    void 비활성_회원은_예외가_발생하고_토큰을_발급하지_않는다() {
        User inactiveUser = createUser();
        inactiveUser.deactivate();
        SocialAccount socialAccount = createSocialAccount(inactiveUser);

        givenAppleVerifiedAndExchanged();
        when(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, APPLE_SUB))
                .thenReturn(Optional.of(socialAccount));

        assertThatThrownBy(() -> socialLoginService.login(REQUEST))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INACTIVE_ACCOUNT);

        verify(tokenIssuer, never()).issue(any());
        assertThat(inactiveUser.getLastLoginAt()).isNull();
        assertThat(socialAccount.getProviderRefreshToken()).isNull();
    }


    @Test
    void Apple_identity_token_검증에_실패하면_토큰을_발급하지_않는다() {
        when(appleTokenVerifier.verify(any())).thenThrow(new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN));

        assertThatThrownBy(() -> socialLoginService.login(REQUEST))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN);

        verifyNoInteractions(appleTokenClient, tokenIssuer, socialAccountRepository, userRepository, transactionManager);
    }


    @Test
    void Apple_authorizationCode_교환_결과에_refresh_token이_없으면_토큰을_발급하지_않는다() {
        when(appleTokenVerifier.verify(any())).thenReturn(APPLE_USER);
        when(appleTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .thenReturn(new AppleTokenResponse("apple-access-token", 3600L, "exchanged-id-token", null, "bearer"));

        assertThatThrownBy(() -> socialLoginService.login(REQUEST))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLE_TOKEN_EXCHANGE_FAILED);

        verifyNoInteractions(tokenIssuer, socialAccountRepository, userRepository, transactionManager);
    }


    @Test
    void authorizationCode_교환으로_받은_사용자가_다르면_토큰을_발급하지_않는다() {
        SocialUserInfo otherAppleUser = new SocialUserInfo(SocialProvider.APPLE, "other-sub", EMAIL, true);
        when(appleTokenVerifier.verify(any())).thenReturn(APPLE_USER, otherAppleUser);
        when(appleTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE)).thenReturn(appleTokenResponse());

        assertThatThrownBy(() -> socialLoginService.login(REQUEST))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN);

        verifyNoInteractions(tokenIssuer, socialAccountRepository, userRepository, transactionManager);
    }


    /**
     * Apple identity token 검증, authorizationCode 교환, refresh token 암호화 성공
     */
    private void givenAppleVerifiedAndExchanged() {
        when(appleTokenVerifier.verify(any())).thenReturn(APPLE_USER);
        when(appleTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE)).thenReturn(appleTokenResponse());
        when(socialTokenCipher.encrypt(APPLE_REFRESH_TOKEN)).thenReturn(ENCRYPTED_APPLE_REFRESH_TOKEN);
    }


    private void givenActiveTerms() {
        Terms termsOfService = mock(Terms.class);
        when(termsOfService.getVersion()).thenReturn("1.0");
        when(termsRepository.findByTypeAndActiveTrueAndDeletedAtIsNull(UserTermsType.TERMS_OF_SERVICE))
                .thenReturn(Optional.of(termsOfService));
        when(termsRepository.findByTypeAndActiveTrueAndDeletedAtIsNull(UserTermsType.PRIVACY_POLICY))
                .thenReturn(Optional.of(mock(Terms.class)));
    }


    private AppleTokenResponse appleTokenResponse() {
        return new AppleTokenResponse("apple-access-token", 3600L, "exchanged-id-token", APPLE_REFRESH_TOKEN, "bearer");
    }


    /**
     * 검증된 가짜 이메일로 유저 생성
     */
    private User createUser() {
        User user = User.createSocialUser(EMAIL, true);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }


    /**
     * 소셜 계정 생성
     */
    private SocialAccount createSocialAccount(User user) {
        return SocialAccount.builder()
                .user(user)
                .provider(SocialProvider.APPLE)
                .providerUserId(APPLE_SUB)
                .providerEmail(EMAIL)
                .build();
    }
}
