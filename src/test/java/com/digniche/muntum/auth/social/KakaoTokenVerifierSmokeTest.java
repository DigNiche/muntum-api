
package com.digniche.muntum.auth.social;

import com.digniche.muntum.auth.dto.request.SocialLoginRequest;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.SocialProvider;

public class KakaoTokenVerifierSmokeTest {

    public static void main(String[] args) {

        // Test 1: Matching IDs and verified email
        KakaoTokenVerifier validVerifier = createVerifier(
                123L,
                new KakaoUserInfoResponse(
                        123L,
                        new KakaoUserInfoResponse.KakaoAccount(
                                "test@example.com",
                                true, true, false
                        )
                )
        );

        SocialUserInfo result = validVerifier.verify(
                kakaoRequest()
        );

        check(result.provider() == SocialProvider.KAKAO,
                "Wrong provider");
        check(result.providerUserId().equals("123"),
                "Wrong user ID");
        check(result.email().equals("test@example.com"),
                "Wrong email");
        check(result.emailVerified(),
                "Email should be verified");

        // Test 2: Different IDs must be rejected
        KakaoTokenVerifier mismatchVerifier = createVerifier(
                123L,
                new KakaoUserInfoResponse(999L, null)
        );

        expectInvalidToken(() ->
                mismatchVerifier.verify(kakaoRequest())
        );

        // Test 3: Missing email must be handled safely
        KakaoTokenVerifier noEmailVerifier = createVerifier(
                123L,
                new KakaoUserInfoResponse(123L, null)
        );

        SocialUserInfo noEmail =
                noEmailVerifier.verify(kakaoRequest());

        check(noEmail.email() == null,
                "Missing email should remain null");
        check(!noEmail.emailVerified(),
                "Missing email cannot be verified");

        // Test 4: Invalid email must not be used
        KakaoTokenVerifier invalidEmailVerifier = createVerifier(
                123L,
                new KakaoUserInfoResponse(
                        123L,
                        new KakaoUserInfoResponse.KakaoAccount(
                                "invalid@example.com",
                                false, true, false
                        )
                )
        );

        SocialUserInfo invalidEmail =
                invalidEmailVerifier.verify(kakaoRequest());

        check(invalidEmail.email() == null,
                "Invalid email should be discarded");

        System.out.println("All 4 smoke tests passed.");
    }

    private static SocialLoginRequest kakaoRequest() {
        return new SocialLoginRequest(
                SocialProvider.KAKAO,
                "fake-token-for-local-testing",
                null,
                null
        );
    }

    private static KakaoTokenVerifier createVerifier(
            long tokenUserId,
            KakaoUserInfoResponse userResponse
    ) {
        KakaoTokenClient fakeTokenClient =
                new KakaoTokenClient(1505605L) {
                    @Override
                    public long verifyAndGetUserId(String token) {
                        return tokenUserId;
                    }
                };

        KakaoUserInfoClient fakeUserClient =
                new KakaoUserInfoClient() {
                    @Override
                    public KakaoUserInfoResponse getUserInfo(
                            String token
                    ) {
                        return userResponse;
                    }
                };

        return new KakaoTokenVerifier(
                fakeTokenClient,
                fakeUserClient
        );
    }

    private static void check(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectInvalidToken(Runnable operation) {
        try {
            operation.run();
        } catch (BusinessException exception) {
            check(
                    exception.getErrorCode()
                            == ErrorCode.INVALID_SOCIAL_TOKEN,
                    "Unexpected error code"
            );
            return;
        }

        throw new AssertionError(
                "Expected INVALID_SOCIAL_TOKEN"
        );
    }
}
