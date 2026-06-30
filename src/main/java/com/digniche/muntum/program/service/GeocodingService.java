package com.digniche.muntum.program.service;

import com.digniche.muntum.program.dto.request.GeoCoordinate;
import com.digniche.muntum.program.dto.request.KakaoGeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * 좌표 반환 서비스
 */
@Service
@RequiredArgsConstructor
public class GeocodingService {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    @Value("${kakao.api.geocoding-url}")
    private String geocodingUrl;

    private final RestTemplate restTemplate;

    public Optional<GeoCoordinate> getCoordinate(String address) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);

        String url = UriComponentsBuilder.fromHttpUrl(geocodingUrl)
                .queryParam("query", address)
                .queryParam("size", 1)
                .build()
                .toUriString();

        ResponseEntity<KakaoGeocodingResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                KakaoGeocodingResponse.class

                /**
                 * {
                 *   "documents": [
                 *     {
                 *       "address_name": "서울 강남구 테헤란로 152",
                 *       "address_type": "ROAD_ADDR",
                 *       "x": "127.036075",   ← 경도
                 *       "y": "37.500613"     ← 위도
                 *     }
                 *   ],
                 *   "meta": { "total_count": 1 }
                 * }
                 */
        );

        return Optional.ofNullable(response.getBody())
                .map(KakaoGeocodingResponse::documents)
                .filter(docs -> !docs.isEmpty())
                .map(docs -> docs.get(0))
                .map(doc -> new GeoCoordinate(
                        Double.parseDouble(doc.y()),   // y = 위도
                        Double.parseDouble(doc.x())    // x = 경도
                ));
    }
}
