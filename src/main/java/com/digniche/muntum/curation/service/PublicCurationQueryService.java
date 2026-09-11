package com.digniche.muntum.curation.service;

import com.digniche.muntum.curation.dto.response.CurationImageResponse;
import com.digniche.muntum.curation.dto.response.PublicCurationDetailResponse;
import com.digniche.muntum.curation.dto.response.PublicCurationSummaryResponse;
import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCurationQueryService {

    private static final List<ProgramStatus> PUBLIC_VIEWABLE_STATUSES =
            List.of(ProgramStatus.ACTIVE, ProgramStatus.ENDED);

    private final CurationRepository curationRepository;
    private final CurationImageService curationImageService;
    private final CuratorProfileService curatorProfileService;
    private final ProgramRepository programRepository;

    public PageResponse<PublicCurationSummaryResponse> getCurations(
            UUID programId,
            int page,
            int size
    ) {
        validatePublicProgram(programId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "reviewedAt")
        );

        Page<Curation> curations =
                curationRepository.findByProgram_IdAndPublicationStatus(
                        programId,
                        CurationPublicationStatus.PUBLISHED,
                        pageable
                );

        List<PublicCurationSummaryResponse> content =
                toSummaries(curations.getContent());

        Page<PublicCurationSummaryResponse> responsePage =
                new PageImpl<>(
                        content,
                        pageable,
                        curations.getTotalElements()
                );

        return PageResponse.from(responsePage);
    }

    public List<PublicCurationSummaryResponse> getAllSummaries(
            UUID programId
    ) {
        List<Curation> curations =
                curationRepository
                        .findAllByProgram_IdAndPublicationStatusOrderByReviewedAtDesc(
                                programId,
                                CurationPublicationStatus.PUBLISHED
                        );

        return toSummaries(curations);
    }

    public PublicCurationDetailResponse getCuration(
            UUID programId,
            UUID curationId
    ) {
        validatePublicProgram(programId);

        Curation curation =
                curationRepository
                        .findByIdAndProgram_IdAndPublicationStatus(
                                curationId,
                                programId,
                                CurationPublicationStatus.PUBLISHED
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.CURATION_NOT_FOUND
                                )
                        );

        CuratorProfileResponse curator =
                curatorProfileService.getCuratorProfile(
                        curation.getCuratorId()
                );

        List<CurationImageResponse> images =
                curationImageService.getOrderedImages(curationId);

        return PublicCurationDetailResponse.from(
                curation,
                curator,
                images
        );
    }

    private List<PublicCurationSummaryResponse> toSummaries(
            List<Curation> curations
    ) {
        List<UUID> curationIds = curations.stream()
                .map(Curation::getId)
                .toList();

        List<UUID> curatorIds = curations.stream()
                .map(Curation::getCuratorId)
                .distinct()
                .toList();

        Map<UUID, CuratorProfileResponse> curatorMap =
                curatorProfileService.getCuratorProfiles(curatorIds);

        Map<UUID, String> thumbnailMap =
                curationImageService.getThumbnailMap(curationIds);

        return curations.stream()
                .map(curation ->
                        PublicCurationSummaryResponse.from(
                                curation,
                                curatorMap.get(curation.getCuratorId()),
                                thumbnailMap.get(curation.getId())
                        )
                )
                .toList();
    }

    private void validatePublicProgram(UUID programId) {
        programRepository
                .findByIdAndDeletedAtIsNullAndStatusIn(
                        programId,
                        PUBLIC_VIEWABLE_STATUSES
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PROGRAM_NOT_FOUND
                        )
                );
    }
}