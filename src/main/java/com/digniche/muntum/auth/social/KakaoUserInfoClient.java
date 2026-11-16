package com.digniche.muntum.auth.social;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class KakaoUserInfoClient {

    private static final String KAKAO_BASE_URL =
            "https://kapi.kakao.com";

    private final RestClient restClient;

    public KakaoUserInfoClient() {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(KAKAO_BASE_URL)
                .requestFactory(factory)
                .build();
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri("/v2/user/me")
                    .headers(headers ->
                            headers.setBearerAuth(accessToken)
                    )
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null
                    || response.id() == null
                    || response.id() <= 0) {

                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }

            return response;

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 401) {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }

            throw new BusinessException(ErrorCode.KAKAO_API_UNAVAILABLE);

        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.KAKAO_API_UNAVAILABLE);
        }
    }
}