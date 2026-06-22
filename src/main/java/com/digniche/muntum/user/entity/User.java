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

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "nickname", unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.AUDIENCE;

    @Column(name = "taste_selected", nullable = false)
    private boolean tasteSelected = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;



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

    /**
     * Claim용 User 객체 생성
     * @param userId
     * @param role
     */
    public static User ofClaims(UUID userId, UserRole role) {
        return new User(userId, role);
    }
    private User(UUID userId, UserRole role) {
        this.id = userId;
        this.role = role;
    }


    // 닉네임 설정 및 수정
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 사용자 계정 상태 변경
    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    // 사용자 계정 활성화 확인
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    // 사용자 계정 비활성화
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    // 사용자 삭제(Soft Deleted)
    public void softDelete(UUID manager) {
        this.status = UserStatus.DELETED;
        this.deletedBy = manager;
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

}
