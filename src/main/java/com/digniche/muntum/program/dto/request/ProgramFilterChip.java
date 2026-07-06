package com.digniche.muntum.program.dto.request;

/**
 * 프로그램 목록 필터 칩
 * - 단일 선택 (한 번에 하나만 활성). 미선택 시 파라미터 생략(null)
 * - 전체/내취향/검색/지도 목록에 공통으로 얹힘
 * - 칩 → 실제 조건 변환은 조회 계층에서 처리 (이 enum은 '어떤 칩인지'만 표현하는 마커)
 */

public enum ProgramFilterChip {
    // 속성 기반 칩
    FREE, // 무료 (program.free = true)
    THIS_WEEK, // 이번주 (기간 overlap — 정의 확정 후 조회단에서 처리)
    NO_RESERVATION, // 예약없이 (program.reserved = false)

    //유형 기반 칩 - 상수명을 ProgramTye과 1:1로 맞춤
    EXHIBITION,
    PERFORMANCE,
    CLASS_EXPERIENCE,
    FAIR,
    // 지금핫한 (스크랩 수 정렬 — 필터 아님, 지도탭 전용)
    HOT;
}