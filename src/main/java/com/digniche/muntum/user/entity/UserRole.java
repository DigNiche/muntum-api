package com.digniche.muntum.user.entity;

/**
 * 사용자 역할 타입 - 관리자/큐레이터/관람객
 */
public enum UserRole {
    MANAGER,    // 관리자
    CURATOR,    // 큐레이터
    AUDIENCE;   // 관람객

    public String toAuthority() {
        return "ROLE_" + this.name();
    }

    // TODO: Role Hierarchy -> Permission 기반으로 변경
}
