package com.digniche.muntum.auth.social;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.time.Duration;

@Slf4j
@Component
public class KakaoTokenClient {

    private static final String KAKAO_BASE_URL =
            "https://kapi.kakao.com";

    private final RestClient restClient;
    private final long expectedAppId;

    public KakaoTokenClient(
            @Value("${social.kakao.app-id}") long expectedAppId
    ) {
        this.expectedAppId = expectedAppId;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(KAKAO_BASE_URL)
                .requestFactory(factory)
                .build();
    }
    public long verifyAndGetUserId(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        try {
            KakaoTokenResponse response = restClient.get()
                    .uri("/v1/user/access_token_info")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null
                    || response.id() == null
                    || response.appId() == null
                    || response.expiresIn() == null
                    || response.expiresIn() <= 0
                    || response.appId().longValue() != expectedAppId) {

                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }

            return response.id();

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 401) {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }

            throw new BusinessException(ErrorCode.KAKAO_API_UNAVAILABLE);

        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.KAKAO_API_UNAVAILABLE);
        }
    }


    /**
     * Kakao 회원 탈퇴 시 서비스 연결 해제
     */
    public void unlink(String accessToken, String expectedProviderUserId) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        try {
            KakaoUnlinkResponse response = restClient.post()
                    .uri("/v1/user/unlink")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve()
                    .body(KakaoUnlinkResponse.class);

            // 연결 해제된 사용자가 실제 탈퇴 요청자인지 확인
            if (response == null
                    || response.id() == null
                    || !Long.toString(response.id()).equals(expectedProviderUserId)) {

                log.error("[KAKAO_UNLINK_INVALID_RESPONSE]");

                throw new BusinessException(ErrorCode.KAKAO_UNLINK_FAILED);
            }

            log.info("[KAKAO_UNLINK_SUCCESS]");

        } catch (RestClientResponseException exception) {

            log.error(
                    "[KAKAO_UNLINK_FAILED] status={}",
                    exception.getStatusCode().value()
            );

            if (exception.getStatusCode().value() == 401) {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }

            throw new BusinessException(ErrorCode.KAKAO_UNLINK_FAILED);

        } catch (RestClientException exception) {

            log.error(
                    "[KAKAO_UNLINK_FAILED] exceptionType={}",
                    exception.getClass().getSimpleName()
            );

            throw new BusinessException(ErrorCode.KAKAO_UNLINK_FAILED);
        }
    }
    private record KakaoUnlinkResponse(Long id) {
    }
}