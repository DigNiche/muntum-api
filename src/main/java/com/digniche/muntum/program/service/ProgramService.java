package com.digniche.muntum.program.service;

import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.ProgramKeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.program.dto.request.GeoCoordinate;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.dto.request.ProgramSortType;
import com.digniche.muntum.program.dto.request.ProgramUpdateRequest;
import com.digniche.muntum.program.dto.response.*;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramKeyword;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 프로그램 비즈니스 로직 계층
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramImageRepository programImageRepository;
    private final ProgramKeywordRepository programKeywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final GeocodingService geocodingService;
    private final ProgramImageService programImageService;
    private final ProgramKeywordService programKeywordService;
    private final KeywordRepository keywordRepository;

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

        // 이미지 및 키워드 저장
        if (files != null && !files.isEmpty()) {
            programImageService.uploadImages(savedProgram, files);
        }
        List<ProgramImageResponse> images = programImageService.getOrderedImages(program.getId());

        programKeywordService.saveKeywords(savedProgram, request.keywordIds());

        List<ProgramKeywordResponse> keywords = programKeywordService.getKeywords(savedProgram.getId()).stream()
                .map(ProgramKeywordResponse::from)
                .toList();
        return ProgramResponse.from(savedProgram, images, keywords);
    }

    /**
     * 프로그램 목록 조회 + 검색 통합 진입점 (합의 1: 서비스 최상단 디스패치)
     * - 텍스트 검색(search)과 키워드 검색(keywordIds)은 동시 사용 불가
     * - 텍스트 검색은 다음 단계에서 구현 (현재는 일반 목록으로 흐름)
     */
    public PageResponse<ProgramCardResponse> getPrograms(
            String search,
            List<UUID> keywordIds,
            ProgramType type,
            ProgramSortType sort,
            Sort.Direction order,
            int page,
            int size
    ) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasKeywords = keywordIds != null && !keywordIds.isEmpty();

        // 상호배제 가드: 텍스트 검색과 키워드 검색 동시 사용 불가
        if (hasSearch && hasKeywords) {
            throw new BusinessException(ErrorCode.INVALID_SEARCH_CONDITION);
        }

        // 분기: 키워드 검색
        if (hasKeywords) {
            return searchProgramsByKeywords(keywordIds, page, size);
        }

        // 분기: 텍스트 검색
        if (hasSearch) {
            return searchProgramsByText(search, page, size);
        }

        // 기본: 일반 목록 (위 분기 다 안걸리면 일반 목록. 정렬 파라미터 적용)
        Pageable pageable = PageRequest.of(page, size, createSort(sort, order));
        Page<Program> programPage = programRepository.findByStatusAndDeletedAtIsNull(ProgramStatus.ACTIVE, pageable);
        return PageResponse.from(toCardResponsePage(programPage));

    }
//    public PageResponse<ProgramListResponse> getPrograms(Pageable pageable)
//            ProgramSortType sort,
//            Sort.Direction order,
//            int page,
//            int size
//    )
//    {
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
//    }

    // 마감일이 이번달인 목록 중 마감일이 오늘 날짜와 가까운 순으로 정렬
    public PageResponse<ProgramCardResponse> getProgramsByClosestEndDate(Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        Page<Program> programPage = programRepository.findByStatusOrderByClosestEndDate(
                ProgramStatus.ACTIVE,
                today,
                monthEnd,
                pageable
        );

        return PageResponse.from(toCardResponsePage(programPage));
    }

    // 인기 키워드를 많이 가진 프로그램 순으로 정렬
    public PageResponse<ProgramCardResponse> getProgramsByHotKeywords(int topN, Pageable pageable) {
        List<UUID> topKeywordIds = userKeywordRepository.findTopKeywords(PageRequest.of(0, topN))
                .stream().map(Keyword::getId).toList();

        if (topKeywordIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }

        Page<Program> programPage = programRepository.findProgramsByKeywordIds(
                ProgramStatus.ACTIVE, topKeywordIds, pageable);

        return PageResponse.from(toCardResponsePage(programPage));
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
        programImageRepository.deleteAllByProgramId(programId);
        programKeywordRepository.deleteAllByProgramId(programId);
        programRepository.delete(program);
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

    //헬퍼
    private Page<ProgramCardResponse> toCardResponsePage(Page<Program> programPage) {
        List<UUID> programIds = programPage.getContent().stream().map(Program::getId).toList();
        Map<UUID, String> thumbnailMap = programImageService.getThumbnailMap(programIds);
        Map<UUID, List<ProgramKeywordResponse>> keywordMap = programKeywordRepository
                .findByProgramIdIn(programIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getProgram().getId(),
                        Collectors.mapping(ProgramKeywordResponse::from, Collectors.toList())
                ));

        return programPage.map(program ->
                ProgramCardResponse.from(
                        program,
                        thumbnailMap.get(program.getId()),
                        keywordMap.getOrDefault(program.getId(), List.of())
                        )
                );
    }

    //키워드 검색(사용자가 칩으로 직접 선택한 keywordIds 기반)
    public PageResponse<ProgramCardResponse> searchProgramsByKeywords(
            List<UUID> keywordIds, int page, int size) {
        //1. 입력 정리: dedupe + 빈값 가드
        List<UUID> distinctIds = keywordIds.stream().distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        //2. 키워드 검증: 없거나 비활성인 UUID 섞였으면 입력단에서 거절
        List<Keyword> found = keywordRepository.findAllByIdInAndActiveTrue(distinctIds);
        if (found.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
        }

        //3. 정렬은 쿼리에 하드코딩 -> Pagable엔 page/size만, sort는 버림
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());

        //4. 조회 + 공통 후처리
        Page<Program> programPage = programRepository.searchProgramsByKeywordIds(
                ProgramStatus.ACTIVE, distinctIds, LocalDate.now(), pageable);

        return PageResponse.from(toCardResponsePage(programPage));
    }

    // 텍스트 검색 (프로그램명/한줄소개/큐레이션 LIKE)
    public PageResponse<ProgramCardResponse> searchProgramsByText(
            String search, int page, int size) {

        // 1. 방어적 가드 (디스패치에서 이미 hasSearch로 걸러지지만 안전하게)
        String trimmed = search.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 2. LIKE 패턴: 특수문자 이스케이프 후 %...%로 감쌈
        String pattern = "%" + escapeLike(trimmed) + "%";

        // 3. 정렬은 쿼리에 하드코딩 → sort 버림
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());

        // 4. 조회 + 공통 후처리
        Page<Program> programPage = programRepository.searchProgramsByText(
                ProgramStatus.ACTIVE, pattern, LocalDate.now(), pageable);

        return PageResponse.from(toCardResponsePage(programPage));
    }

    // LIKE 특수문자 이스케이프 (\ % _)
    private String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}