package com.digniche.muntum.curator.service;

import com.digniche.muntum.curator.dto.request.CuratorApplicationCreateRequest;
import com.digniche.muntum.curator.dto.request.CuratorApplicationStatusUpdateRequest;
import com.digniche.muntum.curator.dto.response.CuratorApplicationCardResponse;
import com.digniche.muntum.curator.dto.response.CuratorApplicationResponse;
import com.digniche.muntum.curator.dto.response.ReviewerProfileResponse;
import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;
import com.digniche.muntum.curator.repository.CuratorApplicationRepository;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.auditor.SpecialUserType;
import com.digniche.muntum.global.auditor.UserDisplayLabels;
import com.digniche.muntum.global.auditor.UserDisplayLookup;
import com.digniche.muntum.global.auditor.UserDisplayNames;
import com.digniche.muntum.global.auditor.UserDisplayResolver;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.dto.response.UserProfileResponse;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 큐레이터 지원하기 서비스
 */
@Service
@RequiredArgsConstructor
public class CuratorApplicationService {

    private final CuratorApplicationRepository curatorApplicationRepository;
    private final UserRepository userRepository;
    private final UserDisplayResolver userDisplayResolver;

    private static final UserDisplayLabels APPLICANT_LABELS = new UserDisplayLabels( // 지원자 칸 문구표
            UserDisplayNames.MUNTUM, UserDisplayNames.ANONYMOUS_CURATOR, UserDisplayNames.ANONYMOUS_AUDIENCE, UserDisplayNames.MUNTUM);
    private static final UserDisplayLabels REVIEWER_LABELS = new UserDisplayLabels( // 검토자 칸 문구표
            UserDisplayNames.MUNTUM, UserDisplayNames.ANONYMOUS_CURATOR, UserDisplayNames.ANONYMOUS_AUDIENCE, UserDisplayNames.MUNTUM);

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
        if (curatorApplicationRepository.existsByApplicantIdAndStatus(applicantId, CuratorApplicationStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_ALREADY_PENDING);
        }

        // 큐레이터 지원서 생성
        CuratorApplication application = curatorApplicationRepository.save(request.toEntity(applicant.getId()));
        return CuratorApplicationResponse.from(application, UserProfileResponse.from(applicant), null);
    }


    /**
     * 관람객의 큐레이터 지원서 수정 (대기 상태에서만 가능)
     */
    @Transactional
    public CuratorApplicationResponse updateCuratorApplication(UUID applicationId, UUID applicantId, CuratorApplicationCreateRequest request) {
        CuratorApplication application = getApplicationById(applicationId);

        if (!isOwner(application, applicantId)) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_ACCESS_DENIED);
        }

        if (application.getStatus() != CuratorApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.CURATOR_APPLICATION_NOT_EDITABLE);
        }

        application.updatePortfolio(request.programName(), request.tagline(), request.curation());

        UserDisplayLookup lookup = userDisplayResolver.load(List.of(application.getApplicantId()));
        return CuratorApplicationResponse.from(application, toApplicantProfileResponse(application.getApplicantId(), lookup), null);
    }


    /**
     * 특정 사용자의 가장 최근의 지원 내역 조회
     * - 지원 이력 없는 경우 null 반환
     */
    @Transactional(readOnly = true)
    public CuratorApplicationCardResponse getLatestApplication(UUID applicantId) {
        return curatorApplicationRepository.findFirstByApplicantIdOrderByCreatedAtDesc(applicantId)
                .map(application -> {
                    UserDisplayLookup lookup = userDisplayResolver.load(List.of(application.getApplicantId()));
                    return CuratorApplicationCardResponse.from(application, toApplicantProfileResponse(application.getApplicantId(), lookup));
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

        // 지원자와 검토자를 한 번에 조회 (대기 상태는 검토자 없음)
        UUID reviewedBy = (application.getStatus() == CuratorApplicationStatus.PENDING) ? null : application.getReviewedBy();
        UserDisplayLookup lookup = userDisplayResolver.load(Arrays.asList(application.getApplicantId(), reviewedBy));

        return CuratorApplicationResponse.from(
                application, toApplicantProfileResponse(application.getApplicantId(), lookup), toReviewerProfileResponse(reviewedBy, lookup));
    }

    /**
     * 특정 사용자의 본인 지원 내역 전체 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CuratorApplicationCardResponse> getMyApplications(UUID applicantId, Pageable pageable) {
        Page<CuratorApplication> applications = getApplicationsByApplicant(applicantId, pageable);
        return toCardResponse(applications);
    }


    /**
     * 관리자의 지원자 지원 내역 전체 목록 조회 (상태 필터 선택적)
     */
    @Transactional(readOnly = true)
    public PageResponse<CuratorApplicationCardResponse> getAllApplications(CuratorApplicationStatus status, Pageable pageable) {
        Page<CuratorApplication> applications =
                (status != null) ? curatorApplicationRepository.findByStatus(status, pageable) : curatorApplicationRepository.findAll(pageable);

        return toCardResponse(applications);
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

        UserDisplayLookup lookup = userDisplayResolver.load(List.of(application.getApplicantId()));
        return CuratorApplicationResponse.from(application, toApplicantProfileResponse(application.getApplicantId(), lookup), reviewerProfile);
    }


    /**
     * 목록 조회 응답 변환
     */
    private PageResponse<CuratorApplicationCardResponse> toCardResponse(Page<CuratorApplication> applications) {
        UserDisplayLookup lookup = userDisplayResolver.load(applications.getContent().stream().map(CuratorApplication::getApplicantId).toList());
        return PageResponse.from(applications.map(
                application -> CuratorApplicationCardResponse.from(application, toApplicantProfileResponse(application.getApplicantId(), lookup))));
    }

    /**
     * 지원자 프로필 응답 변환
     */
    private UserProfileResponse toApplicantProfileResponse(UUID applicantId, UserDisplayLookup lookup) {
        return lookup.findMember(applicantId)
                .map(UserProfileResponse::from)
                .orElseGet(() -> {
                    String nickname = lookup.display(applicantId, APPLICANT_LABELS).nickname();
                    String role = SpecialUserType.from(applicantId).map(type -> type.role().name()).orElse(null);
                    return new UserProfileResponse(null, null, nickname, null, role, null, null);
                });
    }


    /**
     * 검토자 프로필 응답 변환
     */
    private ReviewerProfileResponse toReviewerProfileResponse(UUID reviewerId, UserDisplayLookup lookup) {
        if (reviewerId == null) {
            return null;
        }
        return lookup.findMember(reviewerId)
                .map(reviewer -> ReviewerProfileResponse.from(reviewer.getId(), reviewer.getNickname(), reviewer.getEmail()))
                .orElseGet(() -> ReviewerProfileResponse.from(null, lookup.display(reviewerId, REVIEWER_LABELS).nickname(), null));
    }


    /**
     * 큐레이터 지원 승인 절차
     */
    private void approve(CuratorApplication application, User reviewer) {
        User applicant = getUser(application.getApplicantId());
        application.approve(reviewer);
        applicant.promoteToCurator();
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
        return application.getApplicantId().equals(userId);
    }

    /**
     * 관리자 확인
     */
    private boolean isManager(UserRole userRole) {
        return userRole == UserRole.MANAGER;
    }


    /**
     * DB에서 큐레이터 지원서 조회
     */
    private CuratorApplication getApplicationById(UUID applicationId) {
        return curatorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CURATOR_APPLICATION_NOT_FOUND));
    }

    private Page<CuratorApplication> getApplicationsByApplicant(UUID applicantId, Pageable pageable) {
        return curatorApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId, pageable);
    }

    /**
     * DB에서 사용자 조회
     */
    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}