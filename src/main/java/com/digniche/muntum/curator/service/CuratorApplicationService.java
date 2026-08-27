package com.digniche.muntum.curator.service;

import com.digniche.muntum.curator.dto.request.CuratorApplicationCreateRequest;
import com.digniche.muntum.curator.dto.request.CuratorApplicationStatusUpdateRequest;
import com.digniche.muntum.curator.dto.response.CuratorApplicationResponse;
import com.digniche.muntum.curator.dto.response.ReviewerProfileResponse;
import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;
import com.digniche.muntum.curator.repository.CuratorApplicationRepository;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.digniche.muntum.global.config.AuditorAwareImpl.*;

/**
 * 큐레이터 지원하기 서비스
 */
@Service
@RequiredArgsConstructor
public class CuratorApplicationService {

    private final CuratorApplicationRepository curatorApplicationRepository;
    private final UserRepository userRepository;

    /**
     * 관람객의 큐레이터 지원서 생성
     */
    @Transactional
    public CuratorApplicationResponse createCuratorApplication(UUID applicantId, CuratorApplicationCreateRequest request) {
        User applicant = getUser(applicantId);

        // UserRole이 Audience인지 확인
        if (applicant.getRole() != UserRole.AUDIENCE) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_INVALID_APPLICANT_ROLE);
        }

        // 사용자에게 이미 지원 상태가 PENDING인 지원서가 없는지 확인
        if (curatorApplicationRepository.existsByApplicant_IdAndStatus(applicantId, CuratorApplicationStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_ALREADY_PENDING);
        }

        // 큐레이터 지원서 생성
        CuratorApplication application = curatorApplicationRepository.save(request.toEntity(applicant));
        return CuratorApplicationResponse.from(application, null);
    }


    /**
     * 특정 사용자의 가장 최근의 지원 내역 조회
     * - 지원 이력 없는 경우 null 반환
     */
    @Transactional(readOnly = true)
    public CuratorApplicationResponse getLatestApplication(UUID applicantId) {
        return curatorApplicationRepository.findFirstByApplicant_IdOrderByCreatedAtDesc(applicantId)
                .map(application -> {
                    ReviewerProfileResponse reviewerProfile = (application.getReviewedBy() != null)
                            ? displayReviewerProfile(application.getReviewedBy())
                            : null;
                    return CuratorApplicationResponse.from(application, reviewerProfile);
                })
                .orElse(null);
    }

    /**
     * 특정 사용자의 지원 내역 단건 상세 조회
     */
    @Transactional(readOnly = true)
    public CuratorApplicationResponse getApplication(UUID applicationId, UUID checkerId, UserRole checkerRole) {
        CuratorApplication application = getApplicationById(applicationId);

        // 사용자 권한 확인(관리자 / 큐레이터 본인)
        if (!isOwner(application, checkerId) && !isManager(checkerRole)) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_ACCESS_DENIED);
        }

        ReviewerProfileResponse reviewerProfile = displayReviewerProfile(application.getReviewedBy());

        return CuratorApplicationResponse.from(application, reviewerProfile);
    }

    /**
     * 특정 사용자의 본인 지원 내역 전체 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CuratorApplicationResponse> getMyApplications(UUID applicantId, Pageable pageable) {
        Page<CuratorApplication> applications = getApplicationsByApplicant(applicantId, pageable);
        return PageResponse.from(applications.map(this::toCuratorApplicationResponse));
    }


    /**
     * 관리자의 지원자 지원 내역 전체 목록 조회 (상태 필터 선택적)
     */
    @Transactional(readOnly = true)
    public PageResponse<CuratorApplicationResponse> getAllApplications(CuratorApplicationStatus status, Pageable pageable) {
        Page<CuratorApplication> applications =
                (status != null) ? curatorApplicationRepository.findByStatus(status, pageable) : curatorApplicationRepository.findAll(pageable);

        return PageResponse.from(applications.map(this::toCuratorApplicationResponse));
    }

    /**
     * 큐레이터 지원 승인/반려로 사용자 역할 변경
     */
    @Transactional
    public CuratorApplicationResponse evaluateApplication(UUID applicationId, UUID reviewerId, CuratorApplicationStatusUpdateRequest request) {
        CuratorApplication application = getApplicationById(applicationId);
        User reviewer = getUser(reviewerId);
        ReviewerProfileResponse reviewerProfile = ReviewerProfileResponse.from(reviewer.getId(), reviewer.getNickname(), reviewer.getEmail());

        // 검토자 권한 확인
        if (!isManager(reviewer.getRole())) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_ACCESS_DENIED);
        }

        // 지원 심사 가능 상태 확인
        if (application.getStatus() != CuratorApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_STATUS_CHANGE_DENIED);
        }

        switch (request.status()) {
            case APPROVED -> approve(application, reviewer);
            case REJECTED -> reject(application, reviewer, request.rejectReason());
            default -> throw new BusinessException(ErrorCode.INVALID_CURATOR_APPLICATION_STATUS_TRANSITION);
        }

        return CuratorApplicationResponse.from(application, reviewerProfile);
    }


    /**
     * 검토자 정보
     */
    private ReviewerProfileResponse displayReviewerProfile(UUID reviewerId) {
        if (reviewerId == null) return null;

        if (reviewerId.toString().startsWith(WITHDRAWN_MANAGER_UUID_PREFIX)) {
            return ReviewerProfileResponse.from(reviewerId, MUNTUM_OFFICIAL_NICKNAME, MUNTUM_OFFICIAL_EMAIL);
        } else {
            User reviewer = getUser(reviewerId);
            return ReviewerProfileResponse.from(reviewer.getId(), reviewer.getNickname(), reviewer.getEmail());
        }
    }


    /**
     * 큐레이터 지원 승인 절차
     */
    private void approve(CuratorApplication application, User reviewer) {
        application.approve(reviewer);
        reviewer.promoteToCurator();
    }

    /**
     * 큐레이터 지원 반려 절차
     */
    private void reject(CuratorApplication application, User reviewer, CuratorApplicationRejectReason rejectReason) {
        if (rejectReason == null) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_REASON_REQUIRED);
        }
        application.reject(reviewer, rejectReason);
    }

    /**
     * 큐레이터 지원서 소유자 확인
     * - 큐레이터 지원자 == 현재 로그인한 사용자인 경우 True
     */
    private boolean isOwner(CuratorApplication application, UUID userId) {
        return application.getApplicant() != null
                && application.getApplicant().getId().equals(userId);
    }

    /**
     * 관리자 확인
     */
    private boolean isManager(UserRole userRole) {
        return userRole == UserRole.MANAGER;
    }

    /**
     * 응답 DTO 변환
     */
    private CuratorApplicationResponse toCuratorApplicationResponse(CuratorApplication application) {
        return CuratorApplicationResponse.from(application, displayReviewerProfile(application.getReviewedBy()));
    }


    /**
     * DB에서 큐레이터 지원서 조회
     */
    private CuratorApplication getApplicationById(UUID applicationId) {
        return curatorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CURATOR_APPLICATION_NOT_FOUND));
    }

    private Page<CuratorApplication> getApplicationsByApplicant(UUID applicantId, Pageable pageable) {
        return curatorApplicationRepository.findByApplicant_IdOrderByCreatedAtDesc(applicantId, pageable);
    }

    /**
     * DB에서 사용자 조회
     */
    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}