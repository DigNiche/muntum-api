package com.digniche.muntum.auth.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KakaoUserInfoResponseTest {

    @Test
    void shouldDeserializeKakaoUserInfo() throws Exception {

        String json = """
            {
              "id": 123456789,
              "connected_at": "2026-01-01T00:00:00Z",
              "kakao_account": {
                "email": "example@kakao.com",
                "is_email_valid": true,
                "is_email_verified": true
              }
            }
            """;

        ObjectMapper objectMapper = new ObjectMapper();

        KakaoUserInfoResponse response =
                objectMapper.readValue(
                        json,
                        KakaoUserInfoResponse.class
                );

        assertEquals(123456789L, response.id());
        assertEquals(
                "example@kakao.com",
                response.kakaoAccount().email()
        );
        assertTrue(response.kakaoAccount().emailVerified());
    }
}