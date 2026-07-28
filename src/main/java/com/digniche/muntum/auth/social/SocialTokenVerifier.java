package com.digniche.muntum.auth.social;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.user.entity.SocialProvider;

/**
 * 소셜 토큰 검증 공통 인터페이스
 */
public interface SocialTokenVerifier {

    SocialProvider supports();

    SocialUserInfo verify(SocialLoginRequest request);
}