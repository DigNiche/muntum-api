package com.digniche.muntum.program.service;

import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import com.digniche.muntum.keyword.entity.Keyword;
import com.digniche.muntum.keyword.repository.KeywordRepository;
import com.digniche.muntum.program.entity.Program;
import com.digniche.muntum.program.entity.ProgramKeyword;
import com.digniche.muntum.keyword.repository.ProgramKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 프로그램 - 키워드 연결 저장 / 교체 / 조회 담당
 * - ProgramImageService와 대칭 구조
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramKeywordService {

    private static final int MAX_KEYWORDS = 3;

    private final KeywordRepository keywordRepository;
    private final ProgramKeywordRepository programKeywordRepository;

    /**
     * 키워드 연결 저장 (등록/교체 공통 내부 로직)
     */
    // 빈 리스트는 replaceKeywords에서 "전체 삭제" 의미로 사용됨.
// saveKeywords 단독 호출에서는 저장할 키워드가 없으므로 아무 작업도 하지 않는다.
    @Transactional
    public void saveKeywords(Program program, List<UUID> keywordIds) {
        List<Keyword> keywords = validateAndGetKeywords(keywordIds);
        saveLinks(program, keywords);
    }

    /**
     * 키워드 전체 교체 (PUT용): 기존 삭제 → flush → 새로 저장
     */
    @Transactional
    public void replaceKeywords(Program program, List<UUID> keywordIds) {
        List<Keyword> keywords = validateAndGetKeywords(keywordIds);   // ← 삭제 전에 검증
        programKeywordRepository.deleteAllByProgramId(program.getId());
        programKeywordRepository.flush();
        saveLinks(program, keywords);
    }
    /**
     * 이 프로그램에 붙은 키워드 연결 목록 (상세 태그 노출용)
     */
    public List<ProgramKeyword> getKeywords(UUID programId) {
        return programKeywordRepository.findByProgramId(programId);
    }

    public List<Keyword> retrieveProgramsKeywords() {
        return programKeywordRepository.findAllKeywords();
    }

    /**
     * 검증 + 활성 키워드 조회 (등록/수정 공통)
     */
    // null: 등록 시 키워드 없음, 수정 시에는 호출부에서 미변경으로 처리
    // empty: 연결할 키워드 없음. replaceKeywords에서는 전체 삭제 의미
    private List<Keyword> validateAndGetKeywords(List<UUID> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return List.of();
        }

        List<UUID> distinctIds = keywordIds.stream().distinct().toList();   // ← dedupe 필수

        if (distinctIds.size() > MAX_KEYWORDS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        List<Keyword> keywords = keywordRepository.findAllByIdInAndActiveTrue(distinctIds);

        if (keywords.size() != distinctIds.size()) {   // ← 없는 ID + 비활성 동시 검출
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
        }

        return keywords;
    }

    /**
     * 연결 엔티티 생성 + 저장
     */
    private void saveLinks(Program program, List<Keyword> keywords) {
        if (keywords.isEmpty()) {
            return;
        }
        List<ProgramKeyword> links = keywords.stream()
                .map(keyword -> ProgramKeyword.builder()
                        .program(program)
                        .keyword(keyword)
                        .build())
                .toList();
        programKeywordRepository.saveAll(links);
    }
}
