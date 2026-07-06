package com.digniche.muntum.program.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 지도 뷰포트 바운딩 박스 요청
 * - sw = 남서(좌하단), ne = 북동(우상단)
 */
public record MapBoundsRequest(
        @NotNull(message = "swLat은 필수입니다.")
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        Double swLat,

        @NotNull(message = "swLng은 필수입니다.")
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        Double swLng,

        @NotNull(message = "neLat은 필수입니다.")
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        Double neLat,

        @NotNull(message = "neLng은 필수입니다.")
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        Double neLng
) {
    @AssertTrue(message = "잘못된 지도 영역입니다. (남서 좌표가 북동 좌표보다 작아야 합니다)")
    public boolean isValidBounds() {
        // 필드가 null이면 @NotNull이 잡을 거라 여기선 통과시킴 (중복 에러 방지)
        if (swLat == null || neLat == null || swLng == null || neLng == null) return true;
        return swLat < neLat && swLng < neLng;
    }
}