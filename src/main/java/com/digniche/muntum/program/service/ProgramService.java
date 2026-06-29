package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.dto.request.ProgramUpdateRequest;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.program.dto.response.ProgramResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.program.dto.request.ProgramSortType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 프로그램 비즈니스 로직 계층
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;

    /**
     * 프로그램 등록
     */
    @Transactional
    public ProgramResponse createProgram(ProgramCreateRequest request) {
        validateProgramPeriod(request.startDate(), request.endDate());

        Program program = request.toEntity();
        Program savedProgram = programRepository.save(program);

        return ProgramResponse.from(savedProgram);
    }

    /**
     * 프로그램 목록 조회
     */
    public PageResponse<ProgramListResponse> getPrograms(
            ProgramSortType sort,
            Sort.Direction order,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                createSort(sort, order)
        );

        Page<ProgramListResponse> programPage = programRepository.findByDeletedAtIsNull(pageable)
                .map(ProgramListResponse::from);

        return PageResponse.from(programPage);
    }

    private Sort createSort(ProgramSortType sort, Sort.Direction order) {
        Sort primarySort = Sort.by(order, sort.getProperty());
        // 1차 정렬이 createdAt이면 보조 키로 createdAt을 또 넣으면 중복이라, id만 붙인다.
        if ("createdAt".equals(sort.getProperty())) {
            return primarySort.and(Sort.by(Sort.Direction.DESC, "id"));
        }

        return primarySort
                .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                .and(Sort.by(Sort.Direction.DESC, "id"));
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
        validateProgramPeriod(request.startDate(), request.endDate());

        Program program = getActiveProgram(programId);

        program.update(
                request.title(),
                request.programType(),
                request.tagline(),
                request.curation(),
                request.reserved(),
                request.free(),
                request.price(),
                request.venueName(),
                request.venueMeta(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.officialUrl(),
                request.startDate(),
                request.endDate(),
                request.operatingPeriodMeta(),
                request.operatingHours(),
                request.operatingHoursMeta(),
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
    private void validateProgramPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return;
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_PROGRAM_PERIOD);
        }
    }
}