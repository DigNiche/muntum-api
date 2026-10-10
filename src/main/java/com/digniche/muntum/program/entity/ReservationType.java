package com.digniche.muntum.program.entity;

import java.util.List;

/**
 * 프로그램 예약 방식
 */
public enum ReservationType {
    PRE_REGISTRATION,             // 사전예약
    ON_SITE,                      // 현장예매
    PRE_REGISTRATION_AND_ON_SITE, // 사전예약·현장예매
    FREE_ENTRY;                   // 자유관람

    // '예약없이' 필터에 포함되는 예약 방식 (사전예약 전용·null 제외)
    public static final List<ReservationType> NO_RESERVATION_TYPES =
            List.of(ON_SITE, PRE_REGISTRATION_AND_ON_SITE, FREE_ENTRY);
}
