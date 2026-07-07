package com.digniche.muntum.keyword.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.dto.*;
import com.digniche.muntum.keyword.entity.*;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.keyword.repository.ProgramKeywordRepository;
import com.digniche.muntum.keyword.repository.UserKeywordRepository;
import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ProgramKeywordRepository programKeywordRepository;

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

    // 키워드 삭제
    @Transactional
    public void deleteKeyword(UUID keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));

        userKeywordRepository.deleteAllByKeywordId(keywordId);
        programKeywordRepository.deleteAllByKeywordId(keywordId);
        keywordRepository.delete(keyword);
    }

    // 키워드 전체 조회
    @Transactional(readOnly = true)
    public Page<KeywordResponse> retrieveKeywords(Pageable pageable) {
        return keywordRepository.findAll(pageable)
                .map(KeywordResponse::from);
    }

    // 키워드 단건 조회
    @Transactional(readOnly = true)
    public KeywordResponse retrieveKeyword(UUID keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId).orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));
        return KeywordResponse.from(keyword);
    }

    // 인기 키워드 목록 조회
    @Transactional(readOnly = true)
    public List<KeywordResponse> getTopKeywords(int topN) {
        return userKeywordRepository.findTopKeywords(PageRequest.of(0, topN))
                .stream()
                .map(KeywordResponse::from)
                .toList();
    }

}
