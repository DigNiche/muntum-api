package com.digniche.muntum.auth.social;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AppleTokenClient {

    private static final String APPLE_BASE_URL =
            "https://appleid.apple.com";

    private final String clientId;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final RestClient restClient;

    public AppleTokenClient(
            @Value("${social.apple.client-id}")
            String clientId,
            AppleClientSecretGenerator clientSecretGenerator
    ) {
        this.clientId = clientId;
        this.clientSecretGenerator = clientSecretGenerator;
        this.restClient = RestClient.builder()
                .baseUrl(APPLE_BASE_URL)
                .build();
    }

    /**
     * 일회용 authorizationCode를
     * Apple access/refresh token으로 교환
     */
    public AppleTokenResponse exchangeAuthorizationCode(
            String authorizationCode
    ) {
        if (authorizationCode == null
                || authorizationCode.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("client_id", clientId);
        form.add(
                "client_secret",
                clientSecretGenerator.generate()
        );
        form.add("code", authorizationCode);
        form.add(
                "grant_type",
                "authorization_code"
        );

        try {
            AppleTokenResponse response = restClient.post()
                    .uri("/auth/token")
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);

            if (response == null) {
                throw new BusinessException(
                        ErrorCode.APPLE_TOKEN_EXCHANGE_FAILED
                );
            }

            return response;

        } catch (BusinessException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.APPLE_TOKEN_EXCHANGE_FAILED
            );
        }
    }
    /**
     * 회원 탈퇴 시 Apple refresh token 철회
     */
    public void revokeRefreshToken(
            String refreshToken
    ) {
        if (refreshToken == null
                || refreshToken.isBlank()) {
            throw new BusinessException(
                    ErrorCode.APPLE_TOKEN_REVOKE_FAILED
            );
        }

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("client_id", clientId);
        form.add(
                "client_secret",
                clientSecretGenerator.generate()
        );
        form.add("token", refreshToken);
        form.add(
                "token_type_hint",
                "refresh_token"
        );

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/auth/revoke")
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "[APPLE_REVOKE_SUCCESS] status={}",
                    response.getStatusCode().value()
            );

        } catch (RestClientResponseException exception) {
            log.error(
                    "[APPLE_REVOKE_FAILED] status={}, responseBody={}",
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString()
            );
            throw new BusinessException(
                    ErrorCode.APPLE_TOKEN_REVOKE_FAILED
            );
        } catch (RestClientException exception) {
            log.error(
                    "[APPLE_REVOKE_FAILED] requestError={}",
                    exception.getMessage()
            );

            throw new BusinessException(
                    ErrorCode.APPLE_TOKEN_REVOKE_FAILED
            );
        }
    }
}