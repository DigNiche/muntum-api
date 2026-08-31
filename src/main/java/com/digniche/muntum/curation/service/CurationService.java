package com.digniche.muntum.curation.service;

import com.digniche.muntum.curation.dto.request.CurationCreateRequest;
import com.digniche.muntum.curation.dto.response.*;
import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.service.CuratorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    public PageResponse<CurationListResponse> getMyCurations(UUID curatorId, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Curation> curations = curationRepository.findAllByCuratorId(curatorId, pageable);

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
     * 관리자 상세
     */
    public ManagerCurationDetailResponse
    getCurationForManager(
            UUID curationId
    ) {
        Curation curation =
                curationRepository.findById(curationId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.CURATION_NOT_FOUND
                                )
                        );

        CuratorProfileResponse curator = curatorProfileService.getCuratorProfile(curation.getCuratorId()
                );

        List<CurationImageResponse> images = curationImageService.getOrderedImages(curationId
                );

        return ManagerCurationDetailResponse.from(curation, curator, images);
    }
}
