package com.digniche.muntum.program.dto.request;

import java.util.List;

/**
 * 좌표 반환 Kakao Geocoding API Response
 */
public record KakaoGeocodingResponse(
        List<AddressDocument> documents,
        Meta meta
) {
    public record AddressDocument(
            String address_name,
            String address_type,   // "REGION", "ROAD", "REGION_ADDR", "ROAD_ADDR"
            String x,              // 경도 (longitude)
            String y               // 위도 (latitude)
    ) {}

    public record Meta(
            int total_count
    ) {}
}

