package com.digniche.muntum.scrap.dto.request;

import lombok.Getter;

/**
 * 내 스크랩 목록 정렬 기준
 */
@Getter
public enum ScrapSortType {

    /**
     * 스크랩한 시각 기준 최신순
     */
    SCRAPPED_AT("createdAt"),

    /**
     * 프로그램 등록일 기준
     */
    LATEST("program.createdAt"),

    /**
     * 프로그램 운영 시작일 기준
     */
    START_DATE("program.startDate"),

    /**
     * 프로그램 운영 종료일 기준
     * 단, 마감임박순이 아니라 단순 종료일 정렬이다.
     */
    END_DATE("program.endDate");

    private final String property;

    ScrapSortType(String property) {
        this.property = property;
    }

}