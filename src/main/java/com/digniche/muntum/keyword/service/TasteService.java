package com.digniche.muntum.keyword.service;

import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.dto.SelectKeywordsRequest;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.entity.UserKeyword;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.program.dto.request.ProgramFilterChip;
import com.digniche.muntum.program.dto.response.ProgramCardResponse;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramStatus;
import com.digniche.muntum.program.service.ProgramFilterCondition;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.global.analytics.event.OnboardingCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 취향 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TasteService {

    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 키워드 선택(온보딩 생성 / 마이페이지 수정)
    @Transactional
    public List<KeywordResponse> setTasteKeywords(UUID userId, SelectKeywordsRequest request) {
        List<Keyword> keywords = keywordRepository.findAllByNameInAndActiveTrue(request.selectKeywords());

        if (keywords.size() != request.selectKeywords().size()) {
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
        }

        userKeywordRepository.deleteAllByUserId(userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean wasTasteSelected = user.isTasteSelected();
        boolean tasteSelected = !request.selectKeywords().isEmpty();

        user.updateTasteSelected(tasteSelected);

        List<UserKeyword> userKeywords = keywords.stream()
                .map(k -> UserKeyword.builder().user(user).keyword(k).build())
                .toList();

        userKeywordRepository.saveAll(userKeywords);

        if (!wasTasteSelected && tasteSelected) {
            eventPublisher.publishEvent(new OnboardingCompletedEvent(user.getId(), keywords.size()));
        }
        return keywords.stream().map(KeywordResponse::from).toList();
    }


    // 내 취향 키워드 목록 조회
    @Transactional(readOnly = true)
    public List<KeywordResponse> retrieveSelectedKeywords(UUID userId) {
        return userKeywordRepository.findAllByUserId(userId).stream()
                .map(uk -> KeywordResponse.from(uk.getKeyword()))
                .toList();
    }

}
