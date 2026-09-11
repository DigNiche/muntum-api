package com.digniche.muntum.curation.service;

import com.digniche.muntum.curation.dto.request.CurationCreateRequest;
import com.digniche.muntum.curation.dto.request.CurationChangeRequest;
import com.digniche.muntum.curation.dto.response.*;
import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.service.CuratorProfileService;
import com.digniche.muntum.curation.dto.request.CurationApproveExistingRequest;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.dto.request.ProgramCreateRequest;
import com.digniche.muntum.program.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.digniche.muntum.curation.dto.request.CurationUpdateRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurationService {
    private final CurationRepository curationRepository;
    private final CurationImageService curationImageService;
    private final CuratorProfileService curatorProfileService;
    private final ProgramRepository programRepository;
    private final ProgramService programService;

    /**
     * 큐레이션 등록
     */
    @Transactional
    public CurationDetailResponse createCuration(
            UUID curatorId,
            CurationCreateRequest request,
            List<MultipartFile> files
    ) {
        Curation curation = request.toEntity(curatorId);

        Curation savedCuration = curationRepository.save(curation);

        curationImageService.uploadImages(savedCuration, files);

        List<CurationImageResponse> images = curationImageService.getOrderedImages(savedCuration.getId());

        return CurationDetailResponse.from(savedCuration, images);
    }

    /**
     * 내 큐레이션 목록 조회
     */
    public PageResponse<CurationListResponse> getMyCurations(UUID curatorId, CurationStatus status, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Curation> curations = status == null
        ? curationRepository.findAllByCuratorId(curatorId, pageable)
                : curationRepository.findByCuratorIdAndStatus(
                curatorId,
                status,
                pageable
        );

        Page<CurationListResponse> responses = curations.map(CurationListResponse::from);

        return PageResponse.from(responses);
    }

    /**
     * 내 큐레이션 상세 조회
     */
    public CurationDetailResponse getMyCuration(UUID curationId, UUID curatorId
    ) {
        Curation curation = curationRepository
                        .findByIdAndCuratorId(curationId, curatorId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.CURATION_NOT_FOUND));

        List<CurationImageResponse> images = curationImageService.getOrderedImages(curationId);

        return CurationDetailResponse.from(curation, images);
    }
    /**
     * 관리자 목록
     */
    public PageResponse<ManagerCurationListResponse>
    getCurationsForManager(
            CurationStatus status, int page, int size
    ) {
        CurationStatus targetStatus = status != null ? status : CurationStatus.PENDING;

        Pageable pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
                );

        Page<Curation> curations =
                curationRepository.findAllByStatus(targetStatus, pageable);

        List<UUID> curatorIds =
                curations.getContent()
                        .stream()
                        .map(Curation::getCuratorId)
                        .distinct()
                        .toList();

        Map<UUID, CuratorProfileResponse> curatorMap =
                curatorProfileService.getCuratorProfiles(curatorIds);

        Page<ManagerCurationListResponse> responses =
                curations.map(curation ->
                        ManagerCurationListResponse.from(curation, curatorMap.get(curation.getCuratorId()))
                );

        return PageResponse.from(responses);
    }

    /**
     * 관리자 승인.반려 후 응답 변환 메서드
     */
    private ManagerCurationDetailResponse toManagerDetailResponse(
            Curation curation
    ) {
        CuratorProfileResponse curator = curatorProfileService.getCuratorProfile(curation.getCuratorId());

        List<CurationImageResponse> images = curationImageService.getOrderedImages(curation.getId());

        return ManagerCurationDetailResponse.from(curation, curator, images
        );
    }

    /**
     * 기존 프로그램 연결 승인
     */
    @Transactional
    public ManagerCurationDetailResponse approveExisting(
            UUID curationId,
            UUID managerId,
            CurationApproveExistingRequest request
    ) {
        Curation curation =
                getPendingCurationForUpdate(curationId);

        Program program =
                programRepository
                        .findByIdAndDeletedAtIsNull(
                                request.programId()
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.PROGRAM_NOT_FOUND
                                )
                        );

        boolean alreadyExists =
                curationRepository
                        .existsByProgram_IdAndCuratorId(
                                program.getId(),
                                curation.getCuratorId()
                        );

        if (alreadyExists) {
            throw new BusinessException(
                    ErrorCode.CURATION_ALREADY_EXISTS
            );
        }

        curation.approve(program, managerId);

        return toManagerDetailResponse(curation);
    }

    /**
     * 수정요청
     */
    @Transactional
    public ManagerCurationDetailResponse requestChanges(
            UUID curationId,
            UUID managerId,
            CurationChangeRequest request
    ) {
        Curation curation =
                getChangeRequestableCurationForUpdate(curationId);

        if (request.changeRequestReason() == null
                || request.changeRequestReason().isBlank()) {
            throw new BusinessException(
                    ErrorCode.CHANGE_REQUEST_REASON_REQUIRED
            );
        }

        curation.requestChanges(
                managerId,
                request.changeRequestReason(),
                request.publicationStatus()
        );

        return toManagerDetailResponse(curation);
    }
    /**
     * 관리자 상세
     */
    public ManagerCurationDetailResponse getCurationForManager(
            UUID curationId
    ) {
        Curation curation =
                curationRepository.findById(curationId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.CURATION_NOT_FOUND
                                )
                        );
        return toManagerDetailResponse(curation);
    }

    /**
     * Pending 큐레이션 잠금 조회 공통 메서드
     */
    private Curation getPendingCurationForUpdate(
            UUID curationId
    ) {
        Curation curation =
                curationRepository.findByIdForUpdate(curationId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.CURATION_NOT_FOUND
                                )
                        );

        if (curation.getStatus() != CurationStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.CURATION_ALREADY_REVIEWED
            );
        }

        return curation;
    }

    /**
     * 수정요청용
     */

    private Curation getChangeRequestableCurationForUpdate(
            UUID curationId
    ) {
        Curation curation =
                curationRepository.findByIdForUpdate(curationId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.CURATION_NOT_FOUND
                                )
                        );

        if (curation.getStatus() != CurationStatus.PENDING
                && curation.getStatus() != CurationStatus.APPROVED) {

            throw new BusinessException(
                    ErrorCode.CURATION_ALREADY_REVIEWED
            );
        }

        return curation;
    }

    /**
     * 신규 프로그램 승인 서비스
     */
    @Transactional
    public ManagerCurationDetailResponse approveNew(
            UUID curationId,
            UUID managerId,
            ProgramCreateRequest request,
            List<MultipartFile> files
    ) {
        /*
         * 반드시 프로그램 생성보다 PENDING 상태를 확인
         *
         * 이미 승인된 요청이 다시 들어왔을 때 불필요한 새 프로그램 생성되지 않음
         */
        Curation curation =
                getPendingCurationForUpdate(curationId);

        Program program =
                programService.createProgramWithAssets(request, files);

        curation.approve(program, managerId);

        return toManagerDetailResponse(curation);
    }

    @Transactional
    public CurationDetailResponse updateCuration(
            UUID curationId,
            UUID curatorId,
            CurationUpdateRequest request,
            List<MultipartFile> files
    ) {
        Curation curation = curationRepository
                .findByIdAndCuratorId(curationId, curatorId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.CURATION_NOT_FOUND));

        if (curation.getStatus() != CurationStatus.PENDING
                && curation.getStatus() != CurationStatus.CHANGES_REQUESTED
                && curation.getStatus() != CurationStatus.APPROVED) {

            throw new BusinessException(
                    ErrorCode.CURATION_NOT_EDITABLE
            );
        }

        curation.updateContent(
                request.submittedProgramTitle(),
                request.submittedPlace(),
                request.tagline(),
                request.content()
        );

        if (files != null && !files.isEmpty()) {
            curationImageService.replaceImages(
                    curation,
                    files
            );
        }

        List<CurationImageResponse> images =
                curationImageService.getOrderedImages(
                        curationId
                );

        return CurationDetailResponse.from(
                curation,
                images
        );
    }

    @Transactional
    public void deleteCuration(
            UUID curationId,
            UUID curatorId
    ) {
        Curation curation = curationRepository
                .findByIdAndCuratorId(
                        curationId,
                        curatorId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CURATION_NOT_FOUND
                        )
                );

        if (curation.getStatus() != CurationStatus.PENDING
                && curation.getStatus()
                != CurationStatus.CHANGES_REQUESTED) {

            throw new BusinessException(
                    ErrorCode.CURATION_NOT_DELETABLE
            );
        }

        curationImageService.deleteImages(curationId);

        curationRepository.delete(curation);
    }

    @Transactional
    public CurationDetailResponse resubmitCuration(
            UUID curationId,
            UUID curatorId
    ) {
        Curation curation = curationRepository
                .findByIdAndCuratorId(
                        curationId,
                        curatorId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CURATION_NOT_FOUND
                        )
                );

        curation.resubmit();

        List<CurationImageResponse> images =
                curationImageService.getOrderedImages(
                        curationId
                );

        return CurationDetailResponse.from(
                curation,
                images
        );
    }

    @Transactional
    public void unpublishCuration(
            UUID curationId,
            UUID curatorId
    ) {
        Curation curation =
                curationRepository
                        .findByIdAndCuratorId(
                                curationId,
                                curatorId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.CURATION_NOT_FOUND
                                )
                        );

        curation.unpublish();
    }
}
