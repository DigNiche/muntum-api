package com.digniche.muntum.program.service;

import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.ProgramKeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.program.dto.request.*;
import com.digniche.muntum.program.dto.response.*;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.entity.ProgramType;
import com.digniche.muntum.program.repository.ProgramImageRepository;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.search.service.RecentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.digniche.muntum.program.dto.request.ProgramFilterChip;

import java.math.BigDecimal;
import java.time.DayOfWeek;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramImageRepository programImageRepository;
    private final ProgramKeywordRepository programKeywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final GeocodingService geocodingService;
    private final ProgramImageService programImageService;
    private final ProgramKeywordService programKeywordService;
    private final KeywordRepository keywordRepository;
    private final RecentSearchService recentSearchService;
    private static final int MAP_MAX_RESULTS = 200;

    // 프로그램 등록
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
    @Transactional(readOnly = true)
    public PageResponse<ProgramCardResponse> getPrograms(
            UUID userId,
            String search,
            List<UUID> keywordIds,
            ProgramFilterChip chip,
            ProgramSortType sort,
            Sort.Direction order,
            int page,
            int size
    ) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasKeywords = keywordIds != null && !keywordIds.isEmpty();

        ProgramFilterCondition filter = createFilterCondition(chip);

        // 상호배제 가드: 텍스트 검색과 키워드 검색 동시 사용 불가
        if (hasSearch && hasKeywords) {
            throw new BusinessException(ErrorCode.INVALID_SEARCH_CONDITION);
        }

        // 분기: 키워드 검색
        if (hasKeywords) {
            return searchProgramsByKeywords(keywordIds, filter, page, size);
        }

        // 분기: 텍스트 검색
        if (hasSearch) {
            return searchProgramsByText(userId, search, filter, page, size);
        }

        // 기본: 일반 분기 목록 (위 분기 다 안걸리면 일반 목록. 정렬 파라미터 적용) + chip 조건 적용, 기존 sort/order도 유지
        Pageable pageable = PageRequest.of(page, size, createSort(sort, order));

        Page<Program> programPage = programRepository.findProgramsWithFilter(
                ProgramStatus.ACTIVE,
                filter.freeOnly(),
                filter.noReservationOnly(),
                filter.programType(),
                filter.weekStart(),
                filter.weekEnd(),
                pageable
        );

        return PageResponse.from(toCardResponsePage(programPage));

    }

    /**
     * 목적별 프로그램 목록 조회 : 섹션
     */

    // 프로그램 목록 정렬 조회 : 마감일이 이번달인 목록 중 마감일이 오늘 날짜와 가까운 순으로 정렬
    @Transactional(readOnly = true)
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

    // 프로그램 목록 정렬 조회 : 인기 키워드를 많이 가진 프로그램 순으로 정렬
    @Transactional(readOnly = true)
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
     * 목적별 프로그램 목록 조회 : 지도
     */

    // 입력 좌표로부터 반경 n km 이내의 프로그램 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<ProgramCardResponse> getNearbyPrograms(double lat, double lng, double radiusKm, Pageable pageable) {
        double radiusMeters = radiusKm * 1000;
        Page<Program> programPage = programRepository.findNearbyPrograms(lat, lng, radiusMeters, pageable);
        return PageResponse.from(toCardResponsePage(programPage));
    }

    // 지도 뷰포트 바운딩 박스 기반 프로그램 조회 (필터칩 적용)
        // 페이지네이션 없음: 핀 + 바텀시트가 같은 데이터셋 공유, 상한 200건
    public List<ProgramCardResponse> getProgramsInBounds(MapBoundsRequest bounds, ProgramFilterChip chip) {
        List<Program> programs;

        if (chip == ProgramFilterChip.HOT) {
            programs = programRepository.findHotProgramsInBounds(
                    ProgramStatus.ACTIVE,
                    bounds.swLat(), bounds.swLng(), bounds.neLat(), bounds.neLng(),
                    Limit.of(MAP_MAX_RESULTS));
        } else {
            ProgramFilterCondition filter = createFilterCondition(chip);
            programs = programRepository.findProgramsInBounds(
                    ProgramStatus.ACTIVE,
                    bounds.swLat(), bounds.swLng(), bounds.neLat(), bounds.neLng(),
                    filter.freeOnly(), filter.noReservationOnly(), filter.programType(),
                    filter.weekStart(), filter.weekEnd(),
                    Limit.of(MAP_MAX_RESULTS));
        }
        return toCardResponseList(programs);
    }


    // 헬퍼: toCardResponsePage의 List 버전 (썸네일/키워드 배치 조회 동일)
    private List<ProgramCardResponse> toCardResponseList(List<Program> programs) {
        if (programs.isEmpty()) {
            return List.of();
        }
        List<UUID> programIds = programs.stream().map(Program::getId).toList();
        Map<UUID, String> thumbnailMap = programImageService.getThumbnailMap(programIds);
        Map<UUID, List<ProgramKeywordResponse>> keywordMap = programKeywordRepository
                .findByProgramIdIn(programIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getProgram().getId(),
                        Collectors.mapping(ProgramKeywordResponse::from, Collectors.toList())
                ));

        return programs.stream()
                .map(program -> ProgramCardResponse.from(
                        program,
                        thumbnailMap.get(program.getId()),
                        keywordMap.getOrDefault(program.getId(), List.of())
                ))
                .toList();
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

    // 프로그램 수정
    @Transactional
    public ProgramResponse updateProgram(UUID programId, ProgramUpdateRequest request, List<MultipartFile> files) {
        Program program = getActiveProgramForUpdate(programId);

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

        program.update(
                request.title(), request.programType(), request.tagline(),
                request.curation(), request.reserved(), request.free(),
                request.price(), request.venueName(), request.venueMeta(),
                request.address(),
                request.officialUrl(),
                request.operatingPeriodMeta(),
                request.operatingHours(), request.operatingHoursMeta(),
                request.inquiryContact()
        );

        // 프로그램 키워드 수정
        if (request.keywordIds() != null) {
            programKeywordService.replaceKeywords(program, request.keywordIds());
        }

        // 프로그램 이미지 수정
        if (files != null && !files.isEmpty()) {
            programImageService.replaceImages(program, files); // 반드시 마지막
        }


        List<ProgramImageResponse> images = programImageService.getOrderedImages(programId);

        List<ProgramKeywordResponse> keywords = programKeywordService.getKeywords(programId).stream()
                .map(ProgramKeywordResponse::from)
                .toList();
        return ProgramResponse.from(program, images, keywords);
    }

    // 프로그램 삭제
    @Transactional
    public void deleteProgram(UUID programId, UUID deletedBy) {
        Program program = getActiveProgramForUpdate(programId);
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

    // 삭제되지 않은 프로그램 조회 - Lock 적용
    private Program getActiveProgramForUpdate(UUID programId) {
        return programRepository.findActiveProgramForUpdate(
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
    @Transactional(readOnly = true)
    public PageResponse<ProgramCardResponse> searchProgramsByKeywords(
            List<UUID> keywordIds, ProgramFilterCondition filter, int page, int size) {
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
                ProgramStatus.ACTIVE, distinctIds, LocalDate.now(), filter.freeOnly(), filter.noReservationOnly(), filter.programType(), filter.weekStart(), filter.weekEnd(), pageable);

        return PageResponse.from(toCardResponsePage(programPage));
    }

    // 텍스트 검색 (프로그램명/한줄소개/큐레이션 LIKE)
    @Transactional(readOnly=true)
    public PageResponse<ProgramCardResponse> searchProgramsByText(
            UUID userId, String search, ProgramFilterCondition filter, int page, int size) {

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
                ProgramStatus.ACTIVE, pattern, LocalDate.now(), filter.freeOnly(), filter.noReservationOnly(), filter.programType(), filter.weekStart(), filter.weekEnd(), pageable);
        // 로그인 유저면 최근 검색어 저장 (trimmed 재사용, 게스트=null 제외)
        if (userId != null) {
            recentSearchService.save(userId, trimmed);
        }
        return PageResponse.from(toCardResponsePage(programPage));
    }


    /**
     * 내 취향 프로그램 목록
     * - 유저가 선택한 활성 키워드에 매칭되는 프로그램을, 매칭 개수 많은 순으로
     * - 정렬/필터는 키워드 검색(searchProgramsByKeywordIds)과 동일 로직 재사용
     */
    @Transactional(readOnly = true)
    public PageResponse<ProgramCardResponse> getTastePrograms(
            UUID userId, ProgramFilterChip chip, int page, int size) {

        // 1. 유저의 활성 취향 키워드 ID (비활성/삭제는 SQL에서 이미 제외)
        List<UUID> keywordIds = userKeywordRepository.findActiveKeywordIdsByUserId(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());

        // 2. 취향 키워드 없으면 빈 페이지 (hot과 동일 방어)
        if (keywordIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }

        // 3. 칩 → 필터 조건 (검색/일반목록과 동일 변환)
        ProgramFilterCondition filter = createFilterCondition(chip);

        Page<Program> programPage = programRepository.searchProgramsByKeywordIds(
                ProgramStatus.ACTIVE, keywordIds, LocalDate.now(),
                filter.freeOnly(), filter.noReservationOnly(), filter.programType(),
                filter.weekStart(), filter.weekEnd(),
                pageable                                                    // ← 여기도 같은 거
        );
        return PageResponse.from(toCardResponsePage(programPage));
    }

    // LIKE 특수문자 이스케이프 (\ % _)
    private String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private ProgramFilterCondition createFilterCondition(ProgramFilterChip chip) {
        if (chip == null) {
            return new ProgramFilterCondition(null, null, null, null, null);
        }
        return switch (chip) {
            case HOT -> throw new BusinessException(ErrorCode.INVALID_SEARCH_CONDITION);
            case FREE           -> new ProgramFilterCondition(true, null, null, null, null);
            case NO_RESERVATION -> new ProgramFilterCondition(null, true, null, null, null);
            case THIS_WEEK -> {
                LocalDate today = LocalDate.now();
                LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                yield new ProgramFilterCondition(null, null, null, today, weekEnd);
            }
            case EXHIBITION, PERFORMANCE, CLASS_EXPERIENCE, FAIR ->
                    new ProgramFilterCondition(null, null, ProgramType.valueOf(chip.name()), null, null);
        };
    }
}