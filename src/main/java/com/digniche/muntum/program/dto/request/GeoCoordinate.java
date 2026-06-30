package com.digniche.muntum.program.dto.request;

/**
 * 서비스 로직에서 사용할 좌표 응답 Response
 */
public record GeoCoordinate(
        double latitude,
        double longitude
) {}
