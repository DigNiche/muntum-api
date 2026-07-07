package com.digniche.muntum.program.dto.response;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProgramCardResponse(
        UUID id,
        String title,
        ProgramType programType,
        String tagline,
        boolean reserved,
        boolean free,
        String price,
        String venueName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        int viewCount,
        ProgramStatus status,
        String thumbnailUrl,
        List<ProgramKeywordResponse> keywords,   // 키워드 이름 목록
        boolean ended
) {
    public static ProgramCardResponse from(Program program, String thumbnailUrl, List<ProgramKeywordResponse> keywords) {
        boolean ended = program.getEndDate() != null
                && program.getEndDate().isBefore(LocalDate.now());
        return new ProgramCardResponse(
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
                program.getStatus(),
                thumbnailUrl,
                keywords,
                ended
        );
    }
}