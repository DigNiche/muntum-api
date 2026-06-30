package com.digniche.muntum.program.dto.request;

import com.digniche.muntum.program.entity.ProgramType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프로그램 수정 요청 DTO
 * PUT 방식이므로 수정 가능한 전체 값을 받는다.
 */
public record ProgramUpdateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
        String title,

        @NotNull(message = "프로그램 유형은 필수입니다.")
        ProgramType programType,

        @NotBlank(message = "태그라인은 필수입니다.")
        @Size(max = 255, message = "태그라인은 255자를 넘을 수 없습니다.")
        String tagline,

        @NotBlank(message = "큐레이션 내용은 필수입니다.")
        String curation,

        @NotNull(message = "예약 필요 여부는 필수입니다.")
        Boolean reserved,

        @NotNull(message = "무료 여부는 필수입니다.")
        Boolean free,

        @Size(max = 255, message = "가격 정보는 255자를 넘을 수 없습니다.")
        String price,

        @NotBlank(message = "장소명은 필수입니다.")
        @Size(max = 100, message = "장소명은 100자를 넘을 수 없습니다.")
        String venueName,

        @Size(max = 255, message = "장소 부가 정보는 255자를 넘을 수 없습니다.")
        String venueMeta,

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자를 넘을 수 없습니다.")
        String address,

//        BigDecimal latitude,
//        BigDecimal longitude,

        @Size(max = 500, message = "공식 URL은 500자를 넘을 수 없습니다.")
        String officialUrl,

        @Pattern(
                regexp = "^\\d{4}\\.\\d{2}\\.\\d{2} - \\d{4}\\.\\d{2}\\.\\d{2}$",
                message = "운영 기간은 'YYYY.MM.DD - YYYY.MM.DD' 형식이어야 합니다."
        )
        String operatingPeriod,

        @Size(max = 255, message = "운영 기간 설명은 255자를 넘을 수 없습니다.")
        String operatingPeriodMeta,

        @Size(max = 500, message = "운영 시간은 500자를 넘을 수 없습니다.")
        String operatingHours,

        @Size(max = 255, message = "운영 시간 부가 설명은 255자를 넘을 수 없습니다.")
        String operatingHoursMeta,

        @Size(max = 255, message = "문의처는 255자를 넘을 수 없습니다.")
        String inquiryContact

) {
}