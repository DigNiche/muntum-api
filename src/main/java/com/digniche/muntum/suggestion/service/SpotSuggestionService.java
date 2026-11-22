package com.digniche.muntum.suggestion.service;

import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.auditor.UserDisplayLookup;
import com.digniche.muntum.global.auditor.UserDisplayLabels;
import com.digniche.muntum.global.auditor.UserDisplayNames;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.suggestion.dto.request.SpotSuggestionRequest;
import com.digniche.muntum.suggestion.dto.request.SuggestionStatusUpdateRequest;
import com.digniche.muntum.suggestion.dto.response.SpotSuggestionResponse;
import com.digniche.muntum.suggestion.entity.SpotSuggestion;
import com.digniche.muntum.suggestion.entity.SuggestionStatus;
import com.digniche.muntum.suggestion.repository.SpotSuggestionRepository;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 프로그램 제보 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotSuggestionService {
    private final SpotSuggestionRepository spotSuggestionRepository;
    private final UserRepository userRepository;
    private final UserDisplayResolver userDisplayResolver;

    // 탈퇴 시 검토자(reviewedBy) 화면 문구
    private static final UserDisplayLabels REVIEWER_LABELS
            = new UserDisplayLabels(UserDisplayNames.MUNTUM, UserDisplayNames.ANONYMOUS_CURATOR, UserDisplayNames.ANONYMOUS_AUDIENCE, UserDisplayNames.MUNTUM);
    // 탈퇴 시 제보자(informer) 화면 문구
    private static final UserDisplayLabels INFORMER_LABELS
            = new UserDisplayLabels(UserDisplayNames.ANONYMOUS_INFORMER, UserDisplayNames.ANONYMOUS_INFORMER, UserDisplayNames.ANONYMOUS_INFORMER, UserDisplayNames.MUNTUM);

    /**
     * 제보 등록
     */
    @Transactional
    public SpotSuggestionResponse createSpotSuggestion(UUID userId, SpotSuggestionRequest request) {
        SpotSuggestion suggestion = request.toEntity(userId);
        SpotSuggestion saved = spotSuggestionRepository.save(suggestion);
        return toResponse(saved);
    }

    /**
     * 제보 수정
     */
    @Transactional
    public SpotSuggestionResponse updateSpotSuggestion(UUID suggestionId, UUID userId, SpotSuggestionRequest request) {
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);

        if (suggestion.isDeleted()) {
            throw new BusinessException(ErrorCode.SUGGESTION_NOT_FOUND);
        }

        if (!isOwner(suggestion, userId)) {
            throw new BusinessException(ErrorCode.SUGGESTION_ACCESS_DENIED);
        }
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new BusinessException(ErrorCode.SUGGESTION_NOT_EDITABLE);
        }

        suggestion.update(request.programName(), request.address(), request.reason());
        return toResponse(suggestion);
    }

    /**
     * 제보 상태 변경
     */
    @Transactional
    public SpotSuggestionResponse updateSuggestionStatus(UUID suggestionId, UUID reviewerId, SuggestionStatusUpdateRequest request) {
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);

        // 삭제되지 않은 제보인지 확인
        if (suggestion.isDeleted()) {
            throw new BusinessException(ErrorCode.SUGGESTION_NOT_FOUND);
        }
        // 관리자만 제보 상태 변경 가능
        User reviewer = getUserOrThrow(reviewerId);
        if (!reviewer.getRole().equals(UserRole.MANAGER)) {
            throw new BusinessException(ErrorCode.SUGGESTION_ACCESS_DENIED);
        }
        // 제보 상태 변경 가능 여부 확인
        SuggestionStatus target = request.status();
        if (target == suggestion.getStatus()) {
            throw new BusinessException(ErrorCode.INVALID_SUGGESTION_STATUS_TRANSITION);
        }
        // 검토자의 제보 상태 변경
        switch (target) {
            case REVIEWING -> suggestion.startReviewBy(reviewer.getId());
            case APPROVED -> suggestion.approveBy(reviewer.getId());
            case REJECTED -> suggestion.rejectBy(reviewer.getId());
            case PENDING -> throw new BusinessException(ErrorCode.INVALID_SUGGESTION_STATUS_TRANSITION);
        }

        return toResponse(suggestion);
    }


    /**
     * 제보 삭제
     */
    @Transactional
    public void deleteSpotSuggestion(UUID suggestionId, UUID deletedBy) {
        // 존재하는 제보인지 확인
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);
        // 이미 삭제된 제보인지 확인
        if (suggestion.isDeleted()) {
            throw new BusinessException(ErrorCode.SUGGESTION_NOT_FOUND);
        }
        // 제보 삭제
        suggestion.softDelete(deletedBy);
    }


    /**
     * 제보 단건 조회
     */
    @Transactional(readOnly = true)
    public SpotSuggestionResponse getSpotSuggestion(UUID suggestionId, UUID userId, UserRole userRole) {
        // 존재하는 제보인지 확인
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);
        // 관리자와 제보자만 조회 가능
        boolean isAdmin = userRole == UserRole.MANAGER;
        if (!isOwner(suggestion, userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.SUGGESTION_ACCESS_DENIED);
        }

        return toResponse(suggestion);
    }

    /**
     * 내 제보 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<SpotSuggestionResponse> getMySuggestionList(UUID userId, int page, int size) {
        // 제보 최신 등록 순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // 내 제보 목록 조회
        Page<SpotSuggestion> result = spotSuggestionRepository.findByInformer(userId, pageable);
        return toPageResponse(result);
    }


    /**
     * 전체 제보 목록 조회 : 삭제된 것 제외
     */
    @Transactional(readOnly = true)
    public PageResponse<SpotSuggestionResponse> getSpotSuggestionList(SuggestionStatus statusFilter, int page, int size) {
        // 제보 최신 등록 순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // (삭제되지 않은) 제보 상태 필터링
        Page<SpotSuggestion> result = (statusFilter != null)
                ? spotSuggestionRepository.findByStatusAndDeletedAtIsNull(statusFilter, pageable)
                : spotSuggestionRepository.findAllByDeletedAtIsNull(pageable);
        return toPageResponse(result);
    }

    /**
     * 삭제된 제보 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<SpotSuggestionResponse> getDeletedSpotSuggestionList(int page, int size) {
        // 제보 최신 등록 순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // 삭제된 제보 목록 조회
        Page<SpotSuggestion> result = spotSuggestionRepository.findByDeletedAtIsNotNull(pageable);
        return toPageResponse(result);
    }

    /**
     * 단건 조회 응답 DTO 변환
     */
    private SpotSuggestionResponse toResponse(SpotSuggestion suggestion) {
        // 탈퇴한 제보자/검토자 화면 문구
        UserDisplayLookup displayLookup = userDisplayResolver.load(Arrays.asList(suggestion.getInformer(), suggestion.getReviewedBy()));
        return assembleResponse(suggestion, displayLookup);
    }

    private SpotSuggestionResponse assembleResponse(SpotSuggestion suggestion, UserDisplayLookup displayLookup) {
        return SpotSuggestionResponse.from(suggestion, displayLookup.display(suggestion.getInformer(), INFORMER_LABELS), displayLookup.display(suggestion.getReviewedBy(), REVIEWER_LABELS));
    }


    /**
     * 목록 조회 응답 DTO 변환
     */
    private PageResponse<SpotSuggestionResponse> toPageResponse(Page<SpotSuggestion> page) {
        // 페이지의 제보자·검토자 id를 모아 회원 조회는 한 번 (N+1 방지), 문구는 칸마다 다르게
        List<UUID> userIds = page.getContent().stream()
                .flatMap(suggestion -> Stream.of(suggestion.getInformer(), suggestion.getReviewedBy()))
                .toList();
        UserDisplayLookup displayLookup = userDisplayResolver.load(userIds);
        return PageResponse.from(page.map(suggestion -> assembleResponse(suggestion, displayLookup)));
    }

    /**
     * 제보 소유자 확인
     */
    private boolean isOwner(SpotSuggestion suggestion, UUID userId) {
        return userId != null && userId.equals(suggestion.getInformer());
    }

    /**
     * getReferenceById
     */
    private SpotSuggestion getSuggestionOrThrow(UUID suggestionId) {
        return spotSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUGGESTION_NOT_FOUND));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }


}
