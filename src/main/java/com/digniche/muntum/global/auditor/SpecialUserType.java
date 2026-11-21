package com.digniche.muntum.global.auditor;

import com.digniche.muntum.global.config.AuditorAwareImpl;

import java.util.Optional;
import java.util.UUID;

/**
 * users 테이블에 없는 특수 UUID의 종류
 * - 탈퇴자 UUID: 탈퇴 시 작성자 칸에 넣는 역할별 UUID (AuditorAwareImpl.toWithdrawnUserUuid)
 * - 시스템 UUID: 로그인한 사용자 없이 저장된 기록 (AuditorAwareImpl.SYSTEM_UUID)
 */
public enum SpecialUserType {

    WITHDRAWN_MANAGER,  // 탈퇴한 관리자
    WITHDRAWN_CURATOR,  // 탈퇴한 큐레이터
    WITHDRAWN_AUDIENCE, // 탈퇴한 관람객
    SYSTEM;             // 시스템

    /**
     * 특수 UUID 반환, 일반 회원 UUID 혹은 Null인 경우 empty 반환
     */
    public static Optional<SpecialUserType> from(UUID id) {
        // id가 Null일 때
        if (id == null) { return Optional.empty(); }
        // 시스템
        if (AuditorAwareImpl.SYSTEM_UUID.equals(id)) {
            return Optional.of(SYSTEM);
        }

        String value = id.toString();
        // 탈퇴한 관리자
        if (value.startsWith(AuditorAwareImpl.WITHDRAWN_MANAGER_UUID_PREFIX)) {
            return Optional.of(WITHDRAWN_MANAGER);
        }
        // 탈퇴한 큐레이터
        if (value.startsWith(AuditorAwareImpl.WITHDRAWN_CURATOR_UUID_PREFIX)) {
            return Optional.of(WITHDRAWN_CURATOR);
        }
        // 탈퇴한 관람객
        if (value.startsWith(AuditorAwareImpl.WITHDRAWN_AUDIENCE_UUID_PREFIX)) {
            return Optional.of(WITHDRAWN_AUDIENCE);
        }

        // 일반 회원
        return Optional.empty();
    }
}
