package com.digniche.muntum.program.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로그램 유형
 */
@Getter
@RequiredArgsConstructor
public enum ProgramType {
    EXHIBITION("전시"),       // 전시
    CLASS_EXPERIENCE("클래스/체험"), // 클래스 / 체험
    PERFORMANCE("공연"),
    FAIR("축제/박람회");

    private final String description;
}