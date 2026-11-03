package com.digniche.muntum.program.entity;

import java.util.List;

public enum ProgramStatus {
    ACTIVE,   // 활성
    HIDDEN, // 비활성
    ENDED,   // 운영 종료
    DELETED;   // 삭제

    /**
     * 공개 프로그램
     * (큐레이터/관람객 화면에서 보이는 '운영 중 + 운영 종료')
     * - 큐레이션 좋아요 : 공개 프로그램 + 공개 큐레이션 -> 좋아요 누르기/취소 가능
     */
    public static final List<ProgramStatus> PUBLIC_VIEWABLE = List.of(ACTIVE, ENDED);

    /**
     * 큐레이션 관리 프로그램
     * (큐레이터 화면에서 보이는 '운영 중 + 운영 종료 + 숨김')
     * - 큐레이션 좋아요 : 삭제되지 않은 모든 프로그램과 연결된 큐레이션의 좋아요 정보 조회를 위한 것
     */
    public static final List<ProgramStatus> CURATION_CONNECTED = List.of(ACTIVE, ENDED, HIDDEN);
}