package com.digniche.muntum.scrap.service;

import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.program.dto.response.ProgramListResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.scrap.entity.Scrap;
import com.digniche.muntum.scrap.repository.ScrapRepository;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 스크랩 비즈니스 로직 계층
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    /**
     * 스크랩 등록
     * - 기존 row 없음            → 새로 저장
     * - 기존 row 있고 활성        → 멱등(아무것도 안 함)
     * - 기존 row 있고 삭제됨      → 복구(restore)
     */
    @Transactional
    public void createScrap(UUID userId, UUID programId) {
        User user = getUser(userId);
        Program program = getActiveProgram(programId);

        scrapRepository.findByUserIdAndProgramId(userId, programId)
                .ifPresentOrElse(
                        scrap -> {
                            if (scrap.getDeletedAt() != null) {
                                scrap.restore();
                            }
                            // 활성 상태면 아무것도 안 함 = 멱등 처리
                        },
                        () -> scrapRepository.save(
                                Scrap.builder()
                                        .user(user)
                                        .program(program)
                                        .build()
                        )
                );
    }

    /**
     * 스크랩 해제
     * 멱등 처리: 활성 스크랩이 있을 때만 soft delete, 없거나 이미 삭제됐으면 그냥 성공.
     */
    @Transactional
    public void deleteScrap(UUID userId, UUID programId) {
        scrapRepository.findByUserIdAndProgramId(userId, programId)
                .filter(scrap -> scrap.getDeletedAt() == null)
                .ifPresent(scrap -> scrap.softDelete(userId));
    }

    /**
     * 내 스크랩 목록 조회
     */
    public PageResponse<ProgramListResponse> getMyScraps(UUID userId, Pageable pageable) {
        Page<ProgramListResponse> page = scrapRepository
                .findMyScrapsWithProgram(userId, pageable)
                .map(scrap -> ProgramListResponse.from(scrap.getProgram()));

        return PageResponse.from(page);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Program getActiveProgram(UUID programId) {
        return programRepository.findByIdAndDeletedAtIsNull(programId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROGRAM_NOT_FOUND));
    }
}