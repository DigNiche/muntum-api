package com.digniche.muntum.curationlike.service;

import com.digniche.muntum.curation.entity.Curation;
import com.digniche.muntum.curation.entity.CurationPublicationStatus;
import com.digniche.muntum.curation.repository.CurationRepository;
import com.digniche.muntum.curationlike.dto.response.CurationLikeResponse;
import com.digniche.muntum.curationlike.repository.CurationLikeRepository;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 큐레이션 좋아요 누르기, 취소
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurationLikeService {

    private final CurationRepository curationRepository;
    private final CurationLikeRepository curationLikeRepository;
    private final UserRepository userRepository;

    /**
     * 좋아요
     * - Lock: 실제로 추가됐을 때만 like_count + 1
     */
    @Transactional
    public CurationLikeResponse like(UUID curationId, UUID userId, UserRole userRole) {
        validateLikeableRole(userRole);

        // 존재하는 사용자인지 확인. 사용자 조회 시점을 FK 확인할 때보다 앞당김
        if (userRepository.countByIdForShare(userId) == 0) { // 사용자가 없을 경우
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Curation curation = getLikeableCurationForUpdate(curationId);

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
        Curation curation = getLikeableCurationForUpdate(curationId);

        int deleted = curationLikeRepository.deleteByUserIdAndCurationId(userId, curationId);
        if (deleted > 0) { // 실제로 지워진 경우. 아니면 0
            curationRepository.decreaseLikeCount(curationId);
        }

        return new CurationLikeResponse(curation.getLikeCount() - deleted, false);
    }


    /**
     * 특정 사용자의 '큐레이션 좋아요' 관련 활동을 모두 정리
     * - 사용자의 큐레이션 좋아요 모두 삭제 및 좋아요 수 줄이기
     * - 이 메서드 호출하는 쪽은 같은 트랜잭션에서 사용자 행을 먼저 배타 잠금하는 것이 필요
     * - Lock 순서: 사용자 > 큐레이션 > 좋아요
     */
    @Transactional
    public void clearLikeActivity(UUID userId) {
        // 사용자가 좋아요 한 큐레이션 조회(잠금 X)
        List<UUID> candidateCurationIds = curationLikeRepository.findCurationIdsByUserId(userId);
        if (candidateCurationIds.isEmpty()) {
            return;
        }

        // 큐레이션 행 ID 순서로 배타 잠금
        List<UUID> lockedCurationIds = curationRepository.findAllByIdInForUpdate(candidateCurationIds).stream().map(Curation::getId).toList();

        // 위의 두 조회-잠금 사이에 놓친 것 조회하여 포함 
        List<UUID> likedCurationIds = curationLikeRepository.findAllByUserIdAndCurationIdInForUpdate(userId, lockedCurationIds).stream().map(like -> like.getId().getCurationId()).toList();
        if (likedCurationIds.isEmpty()) {
            return;
        }

        // 확인한 좋아요만 삭제
        curationLikeRepository.deleteByUserIdAndCurationIdIn(userId, likedCurationIds);

        // 그 큐레이션들만 좋아요 수 감소
        curationRepository.decreaseLikeCountIn(likedCurationIds);
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
     * 좋아요 가능한 큐레이션 잠금 조회
     */
    private Curation getLikeableCurationForUpdate(UUID curationId) {
        return curationRepository.findLikeableByIdForUpdate(curationId, CurationPublicationStatus.PUBLISHED, ProgramStatus.PUBLIC_VIEWABLE)
                .orElseThrow(() -> new BusinessException(resolveCurationNotLikeableErrorCode(curationId)));
    }


    /**
     * 좋아요를 할 수 없는 이유에 맞는 에러 코드 분류
     * - 좋아요 가능한 큐레이션 잠금 조회가 비었을 때만 호출
     */
    private ErrorCode resolveCurationNotLikeableErrorCode(UUID curationId) {

        Curation curation = curationRepository.findWithProgramById(curationId).orElse(null);

        // 1. 없거나 삭제된 큐레이션
        if (curation == null || curation.getDeletedAt() != null) {
            return ErrorCode.CURATION_NOT_FOUND;
        }
        // 2. 비공개 큐레이션
        if (curation.getPublicationStatus() == CurationPublicationStatus.UNPUBLISHED) return ErrorCode.UNPUBLISHED_CURATION_NOT_LIKEABLE;

        Program program = curation.getProgram();

        // 3. 삭제된 프로그램
        if (program == null || program.getDeletedAt() != null || program.getStatus() == ProgramStatus.DELETED) {
            return ErrorCode.DELETED_PROGRAM_NOT_LIKEABLE;
        }

        // 4. 숨김 프로그램
        if (program.getStatus() == ProgramStatus.HIDDEN) {
            return ErrorCode.HIDDEN_PROGRAM_NOT_LIKEABLE;
        }

        // 5. 공개 조회 - 이 조회 사이에 다시 공개된 경우
        // TODO: 프로그램 상태(ACTIVE/ENDED/HIDDEN/DELETED)나 큐레이션 상태(PUBLISHED/UNPUBLISHED)에 새로운 값이 추가되는 경우 새로운 분기 필요. 아니면 모두 5번 분기로 내려옴.
        return ErrorCode.CURATION_STATE_CHANGED;
    }
}
