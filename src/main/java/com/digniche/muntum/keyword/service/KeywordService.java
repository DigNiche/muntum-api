package com.digniche.muntum.keyword.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.dto.*;
import com.digniche.muntum.keyword.entity.*;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 키워드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final UserRepository userRepository;

    // 키워드 선택(온보딩 생성 / 마이페이지 수정)
    @Transactional
    public List<KeywordResponse> setTasteKeywords(UUID userId, SelectKeywordsRequest request) {
        List<Keyword> keywords = keywordRepository.findAllByNameInAndActiveTrue(request.selectKeywords());

        if (keywords.size() != request.selectKeywords().size()) {
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
        }

        userKeywordRepository.deleteAllByUserId(userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateTasteSelected(!request.selectKeywords().isEmpty());

        List<UserKeyword> userKeywords = keywords.stream()
                .map(k -> UserKeyword.builder().user(user).keyword(k).build())
                .toList();

        userKeywordRepository.saveAll(userKeywords);

        return keywords.stream().map(KeywordResponse::from).toList();
    }

    // 키워드 등록
    @Transactional
    public KeywordResponse createKeyword(KeywordRequest request) {
        if (keywordRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.KEYWORD_ALREADY_EXISTS);
        }

        String categories = KeywordCategory.validateCategories(request.categories());
        KeywordType keywordType = KeywordType.validateType(request.type());

        Keyword keyword = Keyword.builder()
                .name(request.name())
                .description(request.description())
                .type(keywordType)
                .categories(categories)
                .build();

        keywordRepository.save(keyword);
        return KeywordResponse.from(keyword);
    }

    // 키워드 수정
    @Transactional
    public KeywordResponse updateKeyword(UUID keywordId, KeywordRequest request) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));

        if (keywordRepository.existsByNameAndIdNot(request.name(), keywordId)) {
            throw new BusinessException(ErrorCode.KEYWORD_ALREADY_EXISTS);
        }


        String categories = KeywordCategory.validateCategories(request.categories());
        KeywordType keywordType = KeywordType.validateType(request.type());

        keyword.update(request.name(), request.description(), keywordType, categories);
        return KeywordResponse.from(keyword);
    }


    // 키워드 상태 변경
    @Transactional
    public KeywordActiveResponse updateKeywordStatus(UUID keywordId, KeywordActiveRequest request) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));

        if (request.active()) keyword.activate();
        else keyword.deactivate();

        return KeywordActiveResponse.from(keyword);
    }

}
