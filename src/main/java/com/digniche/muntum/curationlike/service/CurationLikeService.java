package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.curationlike.entity.CurationLikeId;
import com.digniche.muntum.curationlike.repository.CurationLikeRepository;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 큐레이션 좋아요 누르기, 취소, 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurationLikeService {

    private final CurationRepository curationRepository;
    private final CurationLikeRepository curationLikeRepository;

    /**
     * 좋아요
     * - Lock: 실제로 추가됐을 때만 like_count + 1
     */
    @Transactional
    public CurationLikeResponse like(UUID curationId, UUID userId, UserRole userRole) {
        validateLikeableRole(userRole);
        Curation curation = getPublicCurationForUpdate(curationId);

        int inserted = curationLikeRepository.insertIfAbsent(userId, curationId, LocalDateTime.now());
        if (inserted > 0) { // 실제로 추가된 경우. 아니면 0
            curationRepository.increaseLikeCount(curationId);
        }

        return new CurationLikeResponse(curation.getLikeCount() + inserted, true);
    }


    /**
     * 좋아요 취소
     * - Lock: 실제로 지워졌을 때만 like_count - 1
     */
    @Transactional
    public CurationLikeResponse cancelLike(UUID curationId, UUID userId, UserRole userRole) {
        validateLikeableRole(userRole);
        Curation curation = getPublicCurationForUpdate(curationId);

        int deleted = curationLikeRepository.deleteByUserIdAndCurationId(userId, curationId);
        if (deleted > 0) { // 실제로 지워진 경우. 아니면 0
            curationRepository.decreaseLikeCount(curationId);
        }

        return new CurationLikeResponse(curation.getLikeCount() - deleted, false);
    }


    /**
     * 큐레이션별 좋아요 수와 내 상태
     * - 비로그인(userId == null)이면 내 상태를 조회하지 않고 liked = false
     */
    public CurationLikeResponse getLike(UUID curationId, UUID userId) {
        Curation curation = curationRepository.findPublicById(
                                                    curationId, CurationPublicationStatus.PUBLISHED, ProgramStatus.PUBLIC_VIEWABLE_STATUSES
                                              ).orElseThrow(() -> new BusinessException(ErrorCode.CURATION_NOT_FOUND));

        boolean liked = userId != null && curationLikeRepository.existsById(new CurationLikeId(userId, curationId));

        return new CurationLikeResponse(curation.getLikeCount(), liked);
    }


    /**
     * 큐레이션 좋아요 누를 수 있는 사용자 역할 식별(관람객, 큐레이터)
     * - TODO: 추후 역할 계층상 MANAGER와 CURATOR 권한을 분리하면 @PreAuthorize로 막는 것으로 변경
     */
    private void validateLikeableRole(UserRole userRole) {
        if (userRole == UserRole.MANAGER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }


    /**
     * 공개 큐레이션 잠금 조회
     */
    private Curation getPublicCurationForUpdate(UUID curationId) {
        return curationRepository.findPublicByIdForUpdate(
                    curationId, CurationPublicationStatus.PUBLISHED, ProgramStatus.PUBLIC_VIEWABLE_STATUSES
                ).orElseThrow(() -> new BusinessException(ErrorCode.CURATION_NOT_FOUND));
    }
}
