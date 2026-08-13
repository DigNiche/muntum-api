package com.digniche.muntum.program.dto.response;

import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.programreaction.dto.response.ProgramReactionSummaryResponse;
import com.digniche.muntum.user.dto.response.CuratorInfoResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

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
        ProgramStatus status,
        List<ProgramImageResponse> images,
        List<ProgramKeywordResponse> keywords,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ProgramReactionSummaryResponse reaction,
        CuratorInfoResponse curator,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 프로그램 등록·수정·상태 변경 응답 변환
     *
     * 반응 정보 포함 X
     */
    public static ProgramResponse from(Program program, List<ProgramImageResponse> images,
                                       List<ProgramKeywordResponse> keywords) {
        return from(
                program,
                images,
                keywords,
                null,
                null
        );
    }
    /**
     * 프로그램 상세 조회 응답 변환
     */
    public static ProgramResponse from(
            Program program,
            List<ProgramImageResponse> images,
            List<ProgramKeywordResponse> keywords,
            ProgramReactionSummaryResponse reaction,
            CuratorInfoResponse curator
    ) {
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
                program.getStatus(),
                images,
                keywords,
                reaction,
                curator,
                program.getCreatedAt(),
                program.getUpdatedAt()
        );
    }
}