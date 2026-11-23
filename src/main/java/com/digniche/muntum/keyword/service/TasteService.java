package com.digniche.muntum.keyword.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.dto.KeywordResponse;
import com.digniche.muntum.keyword.dto.SelectKeywordsRequest;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.entity.UserKeyword;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digniche.muntum.global.analytics.event.OnboardingCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;

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
        // 회원 행 배타 잠금 (회원 본인의 데이터를 바꾸는 트랜잭션은 맨 앞에서 회원 행을 잠근다)
        // - 관심 키워드 삭제·추가 전에 잠가, 같은 회원의 탈퇴(회원 행 → 관심 키워드 CASCADE)와 순서를 맞춘다
        // - 아래에서 회원 행 자체도 수정하므로(tasteSelected) 공유가 아닌 배타 잠금. 같은 회원의 연속 저장은 차례로 처리된다
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Keyword> keywords = keywordRepository.findAllByNameInAndActiveTrue(request.selectKeywords());

        if (keywords.size() != request.selectKeywords().size()) {
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
        }

        userKeywordRepository.deleteAllByUserId(userId);

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
