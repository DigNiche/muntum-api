package com.digniche.muntum.user.service;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.dto.request.TermsCreateRequest;
import com.digniche.muntum.user.dto.request.TermsUpdateRequest;
import com.digniche.muntum.user.dto.response.TermsResponse;
import com.digniche.muntum.user.dto.response.TermsSummaryResponse;
import com.digniche.muntum.user.entity.Terms;
import com.digniche.muntum.user.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 약관 서비스
 */
@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;

    // 약관 등록 (비활성 상태로 저장, 게시는 activate로)
    @Transactional
    public TermsResponse createTerms(TermsCreateRequest request) {
        if (termsRepository.existsByTypeAndVersionAndDeletedAtIsNull(request.type(), request.version())) {
            throw new BusinessException(ErrorCode.TERMS_VERSION_ALREADY_EXISTS);
        }
        Terms terms = termsRepository.save(request.toEntity());
        return TermsResponse.from(terms);
    }

    // 약관 수정 (게시 전 오타/내용 보완용)
    @Transactional
    public TermsResponse updateTerms(UUID termsId, TermsUpdateRequest request) {
        Terms terms = getExistingTerms(termsId);
        terms.update(request.title(), request.content(), request.effectiveAt());
        return TermsResponse.from(terms);
    }

    // 약관 활성화(게시): 같은 타입의 기존 활성 버전은 자동으로 내린다
    @Transactional
    public TermsResponse activateTerms(UUID termsId) {
        Terms terms = getExistingTerms(termsId);

        termsRepository.findByTypeAndActiveTrueAndDeletedAtIsNull(terms.getType())
                .filter(current -> !current.getId().equals(terms.getId()))
                .ifPresent(Terms::deactivate);

        terms.activate();
        return TermsResponse.from(terms);
    }

    // 약관 삭제 (소프트 삭제)
    @Transactional
    public void deleteTerms(UUID termsId, UUID deletedBy) {
        Terms terms = getExistingTerms(termsId);
        terms.softDelete(deletedBy);
    }

    // 현재 게시 중인 약관 목록 (로그인 사용자용)
    @Transactional(readOnly = true)
    public List<TermsSummaryResponse> getActiveTerms() {
        return termsRepository.findAllByDeletedAtIsNull().stream()
                .map(TermsSummaryResponse::from)
                .toList();
    }

    // 약관 단건 조회 (본문 포함)
    @Transactional(readOnly = true)
    public TermsResponse getTerms(UUID termsId) {
        return TermsResponse.from(getExistingTerms(termsId));
    }

    private Terms getExistingTerms(UUID termsId) {
        return termsRepository.findByIdAndDeletedAtIsNull(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));
    }
}