package com.digniche.muntum.program.entity;

import java.util.List;

public enum ProgramStatus {
    ACTIVE,   // 활성
    HIDDEN, // 비활성
    ENDED,   // 운영 종료
    DELETED;   // 삭제

    // 공개 화면에서 프로그램과 그 큐레이션을 볼 수 있는 상태 (운영중+운영종료)
    public static final List<ProgramStatus> PUBLIC_VIEWABLE_STATUSES = List.of(ACTIVE, ENDED);
}