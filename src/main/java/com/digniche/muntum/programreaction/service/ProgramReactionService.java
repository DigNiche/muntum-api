package com.digniche.muntum.programreaction.service;

import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.repository.ProgramKeywordRepository;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.dto.response.ProgramKeywordResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.repository.ProgramRepository;
import com.digniche.muntum.program.service.ProgramImageService;
import com.digniche.muntum.programreaction.dto.request.ReactionState;
import com.digniche.muntum.programreaction.dto.response.ProgramReactionUpdateResponse;
import com.digniche.muntum.programreaction.entity.ProgramReaction;
import com.digniche.muntum.programreaction.entity.ReactionType;
import com.digniche.muntum.programreaction.repository.ProgramReactionRepository;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.programreaction.dto.response.ProgramReactionSummaryResponse;
import com.digniche.muntum.programreaction.repository.ProgramReactionCountProjection;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 프로그램 좋아요·싫어요 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramReactionService {

    private static final List<ProgramStatus> REACTABLE_STATUSES =
            List.of(ProgramStatus.ACTIVE, ProgramStatus.ENDED);

    private final ProgramReactionRepository programReactionRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final ProgramImageService programImageService;
    private final ProgramKeywordRepository programKeywordRepository;

    /**
     * 프로그램 반응을 요청한 최종 상태로 변경
     *
     * LIKE / DISLIKE:
     * - 기존 반응이 없으면 새로 저장
     * - 기존 반응과 다르면 타입 변경
     * - 기존 반응과 같아도 코멘트는 수정 가능
     *
     * NONE:
     * - 반응과 코멘트를 포함한 기록 삭제
     */
    @Transactional
    public ProgramReactionUpdateResponse updateReaction(
            UUID userId,
            UUID programId,
            ReactionState reactionState,
            String comment
    ) {
        Program program = getReactableProgram(programId);

        return switch (reactionState) {
            case LIKE -> setReaction(
                    userId,
                    program,
                    ReactionType.LIKE,
                    comment
            );

            case DISLIKE -> setReaction(
                    userId,
                    program,
                    ReactionType.DISLIKE,
                    comment
            );

            case NONE -> removeReaction(
                    userId,
                    programId
            );
        };
    }

    /**
     * LIKE 또는 DISLIKE 설정
     */
    private ProgramReactionUpdateResponse setReaction(
            UUID userId,
            Program program,
            ReactionType newType,
            String comment
    ) {
        ProgramReaction reaction = programReactionRepository
                .findByUserIdAndProgramId(userId, program.getId())
                .map(existingReaction -> {
                    existingReaction.changeType(newType);
                    existingReaction.changeComment(comment);
                    return existingReaction;
                })
                .orElseGet(() ->
                        createReaction(userId, program, newType, comment)
                );

        return ProgramReactionUpdateResponse.from(reaction.getReactionType(), reaction.getComment());
    }

    /**
     * 반응과 코멘트 최초 등록
     */
    private ProgramReaction createReaction(
            UUID userId,
            Program program,
            ReactionType reactionType,
            String comment
    ) {
        User user = getUser(userId);

        ProgramReaction reaction = ProgramReaction.builder()
                .user(user)
                .program(program)
                .reactionType(reactionType)
                .build();

        reaction.changeComment(comment);

        return programReactionRepository.save(reaction);
    }

    /**
     * 반응과 코멘트를 포함한 기록을 삭제
     */
    private ProgramReactionUpdateResponse removeReaction(
            UUID userId,
            UUID programId
    ) {
        programReactionRepository.deleteByUserIdAndProgramId(
                userId,
                programId
        );

        return ProgramReactionUpdateResponse.from(null, null);
    }

    /**
     * 좋아요·싫어요가 가능한 공개 프로그램 조회
     */
    private Program getReactableProgram(UUID programId) {
        return programRepository
                .findByIdAndDeletedAtIsNullAndStatusIn(
                        programId,
                        REACTABLE_STATUSES
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PROGRAM_NOT_FOUND)
                );
    }

    /**
     * 사용자 조회
     */
    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );
    }
    /**
     * 내 좋아요 또는 싫어요 프로그램 목록 조회
     */
    public PageResponse<ProgramCardResponse> getMyReactions(
            UUID userId,
            ReactionType reactionType,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<ProgramReaction> reactionPage =
                programReactionRepository.findMyReactionsWithProgram(
                        userId,
                        reactionType,
                        REACTABLE_STATUSES,
                        pageable
                );

        List<UUID> programIds = reactionPage.getContent()
                .stream()
                .map(reaction -> reaction.getProgram().getId())
                .toList();

        Map<UUID, String> thumbnailMap =
                programImageService.getThumbnailMap(programIds);

        Map<UUID, List<ProgramKeywordResponse>> keywordMap =
                programKeywordRepository.findByProgramIdIn(programIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                programKeyword ->
                                        programKeyword.getProgram().getId(),
                                Collectors.mapping(
                                        ProgramKeywordResponse::from,
                                        Collectors.toList()
                                )
                        ));

        Page<ProgramCardResponse> responsePage =
                reactionPage.map(reaction -> {
                    Program program = reaction.getProgram();

                    return ProgramCardResponse.from(
                            program,
                            thumbnailMap.get(program.getId()),
                            keywordMap.getOrDefault(
                                    program.getId(),
                                    List.of()
                            )
                    );
                });

        return PageResponse.from(responsePage);
    }
    /**
     * 프로그램 상세 화면에 사용할 반응 정보 조회
     *
     * 비로그인 사용자는 userId가 null이며,
     * 이 경우 myReaction과 myComment가 null로 반환한다.
     */
    public ProgramReactionSummaryResponse getReactionSummary(
            UUID programId,
            UUID userId
    ) {
        long likeCount = 0L;
        long dislikeCount = 0L;

        List<ProgramReactionCountProjection> countResults =
                programReactionRepository
                        .countByProgramIdGroupByReactionType(programId);

        for (ProgramReactionCountProjection countResult : countResults) {
            if (countResult.getReactionType() == ReactionType.LIKE) {
                likeCount = countResult.getReactionCount();
            }

            if (countResult.getReactionType() == ReactionType.DISLIKE) {
                dislikeCount = countResult.getReactionCount();
            }
        }

        ReactionType myReaction = null;
        String myComment = null;

        if (userId != null) {
            ProgramReaction myRecord = programReactionRepository
                    .findByUserIdAndProgramId(userId, programId)
                    .orElse(null);

            if (myRecord != null) {
                myReaction = myRecord.getReactionType();
                myComment = myRecord.getComment();
            }
        }
        return new ProgramReactionSummaryResponse(
                myReaction,
                likeCount,
                dislikeCount,
                myComment
        );
    }
}