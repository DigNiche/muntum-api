package com.digniche.muntum.program.dto.request;

/**
 * 프로그램 목록 정렬 기준
 */
public enum ProgramSortType {

    /**
     * 등록일 기준 최신순
     */
    LATEST("createdAt"),

    /**
     * 조회수 기준
     */
    VIEW("viewCount"),

    /**
     * 운영 시작일 기준
     */
    START_DATE("startDate"),

    /**
     * 운영 종료일 기준
     * 단, 이것은 '마감임박순'이 아니라 단순 종료일 정렬이다.
     */
    END_DATE("endDate");

    private final String property;

    ProgramSortType(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}