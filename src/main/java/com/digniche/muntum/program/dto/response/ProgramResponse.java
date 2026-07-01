package com.digniche.muntum.program.dto.response;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 프로그램 단건 조회 응답 DTO (상세)
 */
public record ProgramResponse(
        UUID id,
        String title,
        ProgramType programType,
        String tagline,
        String curation,
        boolean reserved,
        boolean free,
        String price,
        String venueName,
        String venueMeta,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String officialUrl,
        LocalDate startDate,
        LocalDate endDate,
        String operatingPeriodMeta,
        String operatingHours,
        String operatingHoursMeta,
        String inquiryContact,
        int viewCount,
        ProgramStatus status,
        List<ProgramImageResponse> images,
        List<ProgramKeywordResponse> keywords,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 엔티티 -> 응답 DTO 변환
     */
    public static ProgramResponse from(Program program, List<ProgramImageResponse> images,
                                       List<ProgramKeywordResponse> keywords) {
        return new ProgramResponse(
                program.getId(),
                program.getTitle(),
                program.getProgramType(),
                program.getTagline(),
                program.getCuration(),
                program.isReserved(),
                program.isFree(),
                program.getPrice(),
                program.getVenueName(),
                program.getVenueMeta(),
                program.getAddress(),
                program.getLatitude(),
                program.getLongitude(),
                program.getOfficialUrl(),
                program.getStartDate(),
                program.getEndDate(),
                program.getOperatingPeriodMeta(),
                program.getOperatingHours(),
                program.getOperatingHoursMeta(),
                program.getInquiryContact(),
                program.getViewCount(),
                program.getStatus(),
                images,
                keywords,
                program.getCreatedAt(),
                program.getUpdatedAt()
        );
    }
}