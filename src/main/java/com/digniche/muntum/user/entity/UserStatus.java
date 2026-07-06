package com.digniche.muntum.user.entity;

/**
 * 사용자 상태 관리
 */
public enum UserStatus {
    ACTIVE,     // 계정 활성화
    PENDING,    // 미인증
    INACTIVE,   // 장기 미접속 등 비활성화
    SUSPENDED,  // 정지
    DELETED;    // Soft 삭제
}
