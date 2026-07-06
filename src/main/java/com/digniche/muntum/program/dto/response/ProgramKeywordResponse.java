package com.digniche.muntum.program.dto.response;

import com.digniche.muntum.program.entity.ProgramKeyword;
import com.digniche.muntum.keyword.entity.Keyword;

import java.util.UUID;

/**
 * 프로그램 상세의 키워드 태그 응답
 * - 화면 표시엔 name, 수정 화면 프리필엔 id, 비활성 판단엔 active를 사용
 */
public record ProgramKeywordResponse(
        UUID id,
        String name,
        boolean active
) {
    public static ProgramKeywordResponse from(ProgramKeyword pk) {
        Keyword keyword = pk.getKeyword();

        return new ProgramKeywordResponse(
                keyword.getId(),
                keyword.getName(),
                keyword.isActive()
        );
    }
}