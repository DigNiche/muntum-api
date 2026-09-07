package com.digniche.muntum.curator.entity;

/**
 * 큐레이터 신청 상태
 * - Audience 유저가 큐레이터로 전환될 때 작성한 신청서의 상태를 나타냄
 */
public enum CuratorApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
