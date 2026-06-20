package com.digniche.muntum.user.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 엔티티 - 사용자 프로필 정보
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column
    private LocalDateTime emailVerifiedAt;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(unique = true, length = 50)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.AUDIENCE;

    @Column(nullable = false)
    private boolean tasteSelected = false;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private LocalDateTime deletedAt;



    /**
     * 사용자 엔티티 생성
     *
     * @param email 사용자 이메일
     * @param password 암호화된 비밀번호
     * @param role 사용자 역할 (MANAGER, CURATOR, AUDIENCE)
     */
    @Builder
    public User(String email, String password, UserRole role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    private User(UUID userId, UserRole role) {
        this.id = userId;
        this.role = role;
    }
    public static User ofClaims(UUID userId, UserRole role) {
        return new User(userId, role);
    }

    // 닉네임 설정 및 수정
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 사용자 계정 상태 변경
    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    // 사용자 계정 활성화
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    // 사용자 계정 비활성화
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    // 사용자 삭제(Soft Deleted)
    public void softDelete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // 이메일 인증 완료
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    // 마지막 로그인 시간 변경
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // 취향 키워드 설정 완료
    public void updateTasteSelected() {
        this.tasteSelected = true;
    }



    // 사용자 ID 조회
    public UUID getUserId() {
        return this.id;
    }

}
