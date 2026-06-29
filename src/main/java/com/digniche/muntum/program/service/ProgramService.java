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
import com.digniche.muntum.program.entity.ProgramImage;
import com.digniche.muntum.program.repository.ProgramImageRepository;

import java.util.List;

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
    private final ProgramImageRepository programImageRepository;

    /**
     * 프로그램 등록
     */
    @Transactional
    public ProgramResponse createProgram(ProgramCreateRequest request) {
        validateProgramPeriod(request.startDate(), request.endDate());

        Program program = request.toEntity();
        Program savedProgram = programRepository.save(program);

        saveProgramImages(savedProgram, request.imageUrls());

        List<String> imageUrls = request.imageUrls() != null
                ? request.imageUrls()
                : List.of();          // null이면 빈 리스트로

        return ProgramResponse.from(savedProgram, imageUrls);
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

        List<String> imageUrls = programImageRepository
                .findByProgramIdOrderByDisplayOrderAsc(programId)
                .stream()
                .map(ProgramImage::getImageUrl)
                .toList();

        return ProgramResponse.from(program, imageUrls);
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
        // imageUrls가 null이면 이미지 미변경, null이 아니면(빈 배열 포함) 교체
        if (request.imageUrls() != null) {
            programImageRepository.deleteByProgramId(programId);
            programImageRepository.flush();
            saveProgramImages(program, request.imageUrls());
        }

        // 응답은 항상 DB 실제 상태로 (응답-DB 불일치 방지)
        List<String> imageUrls = programImageRepository
                .findByProgramIdOrderByDisplayOrderAsc(programId)
                .stream()
                .map(ProgramImage::getImageUrl)
                .toList();

        return ProgramResponse.from(program, imageUrls);
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
    /**
     * 프로그램 이미지 저장
     * - imageUrls 순서대로 displayOrder를 1부터 부여 (1번 = 썸네일)
     * - 단방향이므로 ProgramImage에 program을 직접 연결해 저장
     */
    private void saveProgramImages(Program program, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;   // 이미지는 선택값이므로 없으면 그냥 끝
        }

        for (int i = 0; i < imageUrls.size(); i++) {
            ProgramImage image = ProgramImage.builder()
                    .program(program)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i + 1)   // 인덱스 0 → order 1
                    .build();
            programImageRepository.save(image);
        }
    }
}