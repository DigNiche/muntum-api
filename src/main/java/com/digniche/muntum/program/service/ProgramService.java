package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.dto.request.GeoCoordinate;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.dto.request.ProgramSortType;
import com.digniche.muntum.program.dto.request.ProgramUpdateRequest;
import com.digniche.muntum.program.dto.response.ProgramImageResponse;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.program.dto.response.ProgramResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.digniche.muntum.program.dto.response.ProgramKeywordResponse;


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
    private final ProgramImageService programImageService;
    private final ProgramKeywordService programKeywordService;   // 추가
    /**
     * 프로그램 등록
     */
    @Transactional
    public ProgramResponse createProgram(ProgramCreateRequest request, List<MultipartFile> files) {
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

        Program savedProgram = programRepository.save(program);

        // 이미지 저장
        if (files != null && !files.isEmpty()) {
            programImageService.uploadImages(savedProgram, files);
        }
        List<ProgramImageResponse> images = programImageService.getOrderedImages(program.getId());

        // TODO: 키워드 설정
        programKeywordService.saveKeywords(savedProgram, request.keywordIds());

        List<ProgramKeywordResponse> keywords = programKeywordService.getKeywords(savedProgram.getId()).stream()
                .map(ProgramKeywordResponse::from)
                .toList();
        return ProgramResponse.from(savedProgram, images, keywords);
    }

    /**
     * 프로그램 목록 조회
     */
    public Page<ProgramListResponse> getPrograms(Pageable pageable) {
        return programRepository.findByStatusAndDeletedAtIsNull(ProgramStatus.ACTIVE, pageable)
                .map(ProgramListResponse::from);
//    public PageResponse<ProgramListResponse> getPrograms(
//            ProgramSortType sort,
//            Sort.Direction order,
//            int page,
//            int size
//    ) {
//        Pageable pageable = PageRequest.of(
//                page,
//                size,
//                createSort(sort, order)
//        );
//        // ① 엔티티 페이지 (변환 전) - 이름: programPage, 타입: Page<Program>
//        // 1. 프로그램 목록 조회 (쿼리 1번)
//        Page<Program> programPage = programRepository.findProgramsEndedLast(
//                List.of(ProgramStatus.ACTIVE, ProgramStatus.ENDED),
//                LocalDate.now(),
//                pageable
//        );
//
//        // 2. 이 페이지 프로그램들의 id 모으기
//        List<UUID> programIds = programPage.getContent().stream()
//                .map(Program::getId)
//                .toList();
//
//        Map<UUID, String> thumbnailMap = programImageService.getThumbnailMap(programIds);
//
//        Page<ProgramListResponse> responsePage = programPage.map(program ->
//                ProgramListResponse.from(program, thumbnailMap.get(program.getId()))
//        );
//        return PageResponse.from(responsePage);
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
        List<ProgramImageResponse> images = programImageService.getOrderedImages(programId);

        List<ProgramKeywordResponse> keywords = programKeywordService.getKeywords(programId).stream()
                .map(ProgramKeywordResponse::from)
                .toList();
        return ProgramResponse.from(program, images, keywords);
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

        List<ProgramImageResponse> images = programImageService.getOrderedImages(programId);

        if (request.keywordIds() != null) {
            programKeywordService.replaceKeywords(program, request.keywordIds());
        }
        List<ProgramKeywordResponse> keywords = programKeywordService.getKeywords(programId).stream()
                .map(ProgramKeywordResponse::from)
                .toList();
        return ProgramResponse.from(program, images, keywords);
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
        return programRepository.findByIdAndDeletedAtIsNullAndStatusIn(
                        programId, List.of(ProgramStatus.ACTIVE, ProgramStatus.ENDED))
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