package com.digniche.muntum.auth.social;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.SocialProvider;
import org.springframework.stereotype.Component;

@Component
public class KakaoTokenVerifier implements SocialTokenVerifier {

    private final KakaoTokenClient tokenClient;
    private final KakaoUserInfoClient userInfoClient;

    public KakaoTokenVerifier(
            KakaoTokenClient tokenClient,
            KakaoUserInfoClient userInfoClient
    ) {
        this.tokenClient = tokenClient;
        this.userInfoClient = userInfoClient;
    }

    @Override
    public SocialProvider supports() {
        return SocialProvider.KAKAO;
    }

    @Override
    public SocialUserInfo verify(SocialLoginRequest request) {

        // 1. Validate request
        if (request == null
                || request.provider() != SocialProvider.KAKAO
                || request.token() == null
                || request.token().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        String accessToken = request.token();

        // 2. Verify access token and application ownership
        long tokenUserId = tokenClient.verifyAndGetUserId(accessToken);

        // 3. Retrieve Kakao user information
        KakaoUserInfoResponse userInfo = userInfoClient.getUserInfo(accessToken);

        // 4. Compare user IDs
        if (tokenUserId <= 0
                || userInfo == null
                || userInfo.id() == null
                || userInfo.id().longValue() != tokenUserId) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        // 5. Extract account information
        KakaoUserInfoResponse.KakaoAccount account = userInfo.kakaoAccount();

        String email = extractUsableEmail(account);

        boolean emailVerified =
                email != null
                        && Boolean.TRUE.equals(account.emailVerified());

        // 6. Convert to Muntum's common social user format
        return new SocialUserInfo(
                SocialProvider.KAKAO,
                Long.toString(tokenUserId),
                email,
                emailVerified
        );
    }
    private String extractUsableEmail(KakaoUserInfoResponse.KakaoAccount account) {

        if (account == null) {return null;}
        if (Boolean.TRUE.equals(account.emailNeedsAgreement())) {return null;}
        if (!Boolean.TRUE.equals(account.emailValid())) {return null;}

        String email = account.email();

        if (email == null || email.isBlank()) {return null;}

        return email.trim();
    }
}