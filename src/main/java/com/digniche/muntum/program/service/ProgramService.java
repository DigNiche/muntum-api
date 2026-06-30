package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.dto.request.GeoCoordinate;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.dto.request.ProgramUpdateRequest;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.program.dto.response.ProgramResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 프로그램 비즈니스 로직 계층
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramImageRepository programImageRepository;
    private final GeocodingService geocodingService;

    /**
     * 프로그램 등록
     */
    @Transactional
    public ProgramResponse createProgram(ProgramCreateRequest request) {
        Program program = request.toEntity();

        if (request.operatingPeriod() != null) {
            List<LocalDate> operatingPeriod = validateProgramPeriod(request.operatingPeriod());
            program.updateOperatingPeriod(operatingPeriod);
        }

        // 프로그램 등록 시 주소 → 좌표 변환
        GeoCoordinate coord = geocodingService.getCoordinate(request.address())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUD));

        program.setLatitude(BigDecimal.valueOf(coord.latitude()));
        program.setLongitude(BigDecimal.valueOf(coord.longitude()));

        // TODO: 키워드 설정
        // TODO: 프로그램 이미지 저장

        Program savedProgram = programRepository.save(program);

        return ProgramResponse.from(savedProgram);
    }

    /**
     * 프로그램 목록 조회
     */
    public Page<ProgramListResponse> getPrograms(Pageable pageable) {
        return programRepository.findByDeletedAtIsNull(pageable)
                .map(ProgramListResponse::from);
    }

    /**
     * 프로그램 단건 조회
     * 조회수 증가가 있으므로 readOnly 트랜잭션이 아니다.
     */
    @Transactional
    public ProgramResponse getProgram(UUID programId) {
        Program program = getActiveProgram(programId);

        program.increaseViewCount();

        return ProgramResponse.from(program);
    }

    /**
     * 프로그램 수정
     */
    @Transactional
    public ProgramResponse updateProgram(UUID programId, ProgramUpdateRequest request) {
        Program program = getActiveProgram(programId);

        if (request.operatingPeriod() != null) {
            List<LocalDate> operatingPeriod = validateProgramPeriod(request.operatingPeriod());
            program.updateOperatingPeriod(operatingPeriod);
        }

        // 프로그램 등록 시 주소 → 좌표 변환
        if (request.address() != null) {
            GeoCoordinate coord = geocodingService.getCoordinate(request.address())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUD));

            program.setLatitude(BigDecimal.valueOf(coord.latitude()));
            program.setLongitude(BigDecimal.valueOf(coord.longitude()));
        }

        // TODO: 키워드 수정
        // TODO: 프로그램 이미지 수정

        program.update(
                request.title(), request.programType(), request.tagline(),
                request.curation(), request.reserved(), request.free(),
                request.price(), request.venueName(), request.venueMeta(),
                request.address(),
                request.officialUrl(),
                request.operatingPeriodMeta(),
                request.operatingHours(), //request.operatingHoursMeta(),
                request.inquiryContact()
        );

        return ProgramResponse.from(program);
    }

    /**
     * 프로그램 삭제
     */
    @Transactional
    public void deleteProgram(UUID programId, UUID deletedBy) {
        Program program = getActiveProgram(programId);
        programImageRepository.findByProgramIdOrderByDisplayOrderAsc(programId)
                .forEach(image -> image.softDelete(deletedBy));
        // TODO: 연관 삭제 (키워드)

        program.softDelete(deletedBy);
    }

    /**
     * 삭제되지 않은 프로그램 조회
     */
    private Program getActiveProgram(UUID programId) {
        return programRepository.findByIdAndDeletedAtIsNull(programId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROGRAM_NOT_FOUND));
    }

    /**
     * 프로그램 기간 검증
     */
    private List<LocalDate> validateProgramPeriod(String operatingPeriod) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String[] parts = operatingPeriod != null ? operatingPeriod.split(" - ") : new String[]{null, null};
        LocalDate startDate = parts[0] != null ? LocalDate.parse(parts[0], formatter) : null;
        LocalDate endDate = parts.length > 1 && parts[1] != null ? LocalDate.parse(parts[1], formatter) : null;

        if (startDate == null || endDate == null) {
            return null;
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_PROGRAM_PERIOD);
        }

        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(startDate);
        dateList.add(endDate);
        return dateList;
    }

    private List<LocalDate> extractFromOperatingPeriod(String operatingPeriod) {
        if (operatingPeriod != null) {
            return validateProgramPeriod(operatingPeriod);
        }
        return null;
    }
}