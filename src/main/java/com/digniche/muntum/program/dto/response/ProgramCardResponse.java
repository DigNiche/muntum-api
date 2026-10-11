package com.digniche.muntum.program.dto.response;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.entity.ReservationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProgramCardResponse(
        UUID id,
        String title,
        ProgramType programType,
        String tagline,
        ReservationType reservationType,
        boolean free,
        String price,
        String venueName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        ProgramStatus status,
        String thumbnailUrl,
        List<ProgramKeywordResponse> keywords,   // 키워드 이름 목록
        boolean ended,
        boolean isNew
) {
    public static ProgramCardResponse from(Program program, String thumbnailUrl, List<ProgramKeywordResponse> keywords) {
        boolean ended = program.getEndDate() != null
                && program.getEndDate().isBefore(LocalDate.now());
        boolean isNew = program.getCreatedAt() != null
                && !program.getCreatedAt()
                .isBefore(LocalDateTime.now().minusWeeks(2));
        return new ProgramCardResponse(
                program.getId(),
                program.getTitle(),
                program.getProgramType(),
                program.getTagline(),
                program.getReservationType(),
                program.isFree(),
                program.getPrice(),
                program.getVenueName(),
                program.getAddress(),
                program.getLatitude(),
                program.getLongitude(),
                program.getStartDate(),
                program.getEndDate(),
                program.getStatus(),
                thumbnailUrl,
                keywords,
                ended,
                isNew
        );
    }
}