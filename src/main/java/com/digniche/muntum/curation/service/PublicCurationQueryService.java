package com.digniche.muntum.curation.service;

import com.digniche.muntum.curation.dto.response.CurationImageResponse;
import com.digniche.muntum.curation.dto.response.PublicCurationDetailResponse;
import com.digniche.muntum.curation.dto.response.PublicCurationSummaryResponse;
import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.curationlike.service.CurationLikeQueryService;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.user.dto.response.CuratorProfileResponse;
import com.digniche.muntum.user.service.CuratorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 공개 큐레이션 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCurationQueryService {

    private final CurationRepository curationRepository;
    private final CurationImageService curationImageService;
    private final CuratorProfileService curatorProfileService;
    private final ProgramRepository programRepository;
    private final CurationLikeQueryService curationLikeQueryService;

    /**
     * 프로그램과 연결된 큐레이션 목록 조회
     */
    public PageResponse<PublicCurationSummaryResponse> getCurations(UUID programId, UUID userId, int page, int size) {
        // 공개된 프로그램인지 확인
        validatePublicProgram(programId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reviewedAt"));

        // 프로그램과 연결된 큐레이션들 중 게시된 것들만 조회
        Page<Curation> curations = curationRepository.findByProgram_IdAndPublicationStatusAndDeletedAtIsNull(
                        programId,
                        CurationPublicationStatus.PUBLISHED,
                        pageable
                );

        List<PublicCurationSummaryResponse> content = toSummaries(curations.getContent(), userId);
        Page<PublicCurationSummaryResponse> responsePage = new PageImpl<>(content, pageable, curations.getTotalElements());

        return PageResponse.from(responsePage);
    }


    public List<PublicCurationSummaryResponse> getAllSummaries(UUID programId, UUID userId) {
        List<Curation> curations = curationRepository.findAllByProgram_IdAndPublicationStatusAndDeletedAtIsNullOrderByReviewedAtDesc(
                                        programId,
                                        CurationPublicationStatus.PUBLISHED
                                    );

        return toSummaries(curations, userId);
    }


    public PublicCurationDetailResponse getCuration(UUID programId, UUID curationId, UUID userId) {
        validatePublicProgram(programId);

        Curation curation = curationRepository.findByIdAndProgram_IdAndPublicationStatusAndDeletedAtIsNull(
                                                    curationId,
                                                    programId,
                                                    CurationPublicationStatus.PUBLISHED
                                             )
                                             .orElseThrow(() -> new BusinessException(ErrorCode.CURATION_NOT_FOUND));

        CuratorProfileResponse curator = curatorProfileService.getCuratorProfile(curation.getCuratorId());

        List<CurationImageResponse> images = curationImageService.getOrderedImages(curationId);
        CurationLikeResponse like = curationLikeQueryService.getLike(curation, userId);

        return PublicCurationDetailResponse.from(curation, curator, images, like);
    }


    private List<PublicCurationSummaryResponse> toSummaries(List<Curation> curations, UUID userId) {
        List<UUID> curationIds = curations.stream().map(Curation::getId).toList();
        List<UUID> curatorIds = curations.stream().map(Curation::getCuratorId).distinct().toList();

        Map<UUID, CuratorProfileResponse> curatorMap =curatorProfileService.getCuratorProfiles(curatorIds);
        Map<UUID, String> thumbnailMap = curationImageService.getThumbnailMap(curationIds);
        Map<UUID, CurationLikeResponse> likeMap = curationLikeQueryService.getLikes(curations, userId);

        return curations.stream().map(curation ->
                        PublicCurationSummaryResponse.from(
                                curation,
                                curatorMap.get(curation.getCuratorId()),
                                thumbnailMap.get(curation.getId()),
                                likeMap.get(curation.getId())
                        )
                )
                .toList();
    }

    // 프로그램 유효성 검증 : 삭제되지 않은 / 공개된 프로그램인지
    private void validatePublicProgram(UUID programId) {
        programRepository.findByIdAndDeletedAtIsNullAndStatusIn(programId, ProgramStatus.PUBLIC_VIEWABLE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROGRAM_NOT_FOUND));
    }
}