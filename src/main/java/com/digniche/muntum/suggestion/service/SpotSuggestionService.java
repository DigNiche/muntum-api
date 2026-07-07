package com.digniche.muntum.suggestion.service;

import com.digniche.muntum.global.PageResponse;
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

import java.util.UUID;

/**
 * 프로그램 제보 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotSuggestionService {
    private final SpotSuggestionRepository spotSuggestionRepository;
    private final UserRepository userRepository;

    // 제보 등록
    @Transactional
    public SpotSuggestionResponse createSpotSuggestion(UUID userId, SpotSuggestionRequest request) {
        User informer = getUserOrThrow(userId);
        SpotSuggestion suggestion = request.toEntity(informer);
        SpotSuggestion saved = spotSuggestionRepository.save(suggestion);
        return SpotSuggestionResponse.from(saved);
    }

    // 제보 수정
    @Transactional
    public SpotSuggestionResponse updateSpotSuggestion(UUID suggestionId, UUID userId, SpotSuggestionRequest request) {
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);

        if (!isOwner(suggestion, userId)) {
            throw new BusinessException(ErrorCode.SUGGESTION_ACCESS_DENIED);
        }
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new BusinessException(ErrorCode.SUGGESTION_NOT_EDITABLE);
        }

        suggestion.update(request.programName(), request.address(), request.reason());
        return SpotSuggestionResponse.from(suggestion);
    }

    // 제보 상태 변경
    @Transactional
    public SpotSuggestionResponse updateSuggestionStatus(UUID suggestionId, UUID reviewerId, SuggestionStatusUpdateRequest request) {
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);
        User reviewer = getUserOrThrow(reviewerId);
        if (!reviewer.getRole().equals(UserRole.MANAGER)) {
            throw new BusinessException(ErrorCode.SUGGESTION_ACCESS_DENIED);
        }
        SuggestionStatus target = request.status();

        if (target == suggestion.getStatus()) {
            throw new BusinessException(ErrorCode.INVALID_SUGGESTION_STATUS_TRANSITION);
        }

        switch (target) {
            case REVIEWING -> suggestion.startReview(reviewer);
            case APPROVED -> suggestion.approve(reviewer);
            case REJECTED -> suggestion.reject(reviewer);
            case PENDING -> throw new BusinessException(ErrorCode.INVALID_SUGGESTION_STATUS_TRANSITION);
        }

        return SpotSuggestionResponse.from(suggestion);
    }

    // 제보 삭제
    @Transactional
    public void deleteSpotSuggestion(UUID suggestionId) {
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);
        spotSuggestionRepository.delete(suggestion);
    }

    // 제보 단건 조회
    @Transactional(readOnly = true)
    public SpotSuggestionResponse getSpotSuggestion(UUID suggestionId, UUID userId, UserRole userRole) {
        SpotSuggestion suggestion = getSuggestionOrThrow(suggestionId);

        boolean isAdmin = userRole == UserRole.MANAGER;
        if (!isOwner(suggestion, userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.SUGGESTION_ACCESS_DENIED);
        }

        return SpotSuggestionResponse.from(suggestion);
    }

    @Transactional(readOnly = true)
    public PageResponse<SpotSuggestionResponse> getMySuggestionList(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SpotSuggestion> result = spotSuggestionRepository.findByInformer_Id(userId, pageable);
        return PageResponse.from(result.map(SpotSuggestionResponse::from));
    }

    // 전체 제보 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<SpotSuggestionResponse> getSpotSuggestionList(SuggestionStatus statusFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SpotSuggestion> result = (statusFilter != null)
                ? spotSuggestionRepository.findByStatus(statusFilter, pageable)
                : spotSuggestionRepository.findAllBy(pageable);
        return PageResponse.from(result.map(SpotSuggestionResponse::from));
    }

    private boolean isOwner(SpotSuggestion suggestion, UUID userId) {
        return suggestion.getInformer() != null
                && suggestion.getInformer().getId().equals(userId);
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
