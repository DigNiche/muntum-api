package com.digniche.muntum.program.dto.response;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 프로그램 목록 응답 DTO
 * 목록 카드에 필요한 요약 정보만 반환한다.
 */
public record ProgramListResponse(

        UUID id,
        String title,
        ProgramType programType,
        String tagline,
        boolean reserved,
        boolean free,
        String price,
        String venueName,
        String address,
        BigDecimal latitude,    // 지도 핀용 - 유지
        BigDecimal longitude,   // 지도 핀용 - 유지
        LocalDate startDate,
        LocalDate endDate,
        int viewCount,           // 조회순 정렬용 - 유지
        String thumbnailUrl,
        boolean ended
) {
    public static ProgramListResponse from(Program program) {
        return from(program, null);
    }

    public static ProgramListResponse from(Program program, String thumbnailUrl) {
        boolean ended = program.getEndDate() != null
                && program.getEndDate().isBefore(LocalDate.now());
        return new ProgramListResponse(
                program.getId(),
                program.getTitle(),
                program.getProgramType(),
                program.getTagline(),
                program.isReserved(),
                program.isFree(),
                program.getPrice(),
                program.getVenueName(),
                program.getAddress(),
                program.getLatitude(),
                program.getLongitude(),
                program.getStartDate(),
                program.getEndDate(),
                program.getViewCount(),
                thumbnailUrl,
                ended
        );
    }
}