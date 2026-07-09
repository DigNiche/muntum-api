package com.digniche.muntum.user.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import com.digniche.muntum.user.entity.UserRole;
import com.digniche.muntum.user.entity.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
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
    private LocalDateTime lastLoginAt = null;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt = null;

    @Column(name = "deleted_by")
    private UUID deletedBy = null;

    @Column(name = "reactivated_at")
    private LocalDateTime reactivatedAt = null;


    @Builder
    public User(
            String email,
            String password,
            UserRole role
    ) {
        this.email = email;
        this.password = password;
        this.role = role != null ? role : UserRole.AUDIENCE;
        this.status = UserStatus.ACTIVE;
        this.emailVerified = false;
        this.tasteSelected = false;
    }

    /**
     * Claim용 User 객체 생성
     */
    private User(UUID userId, UserRole role) {
        this.id = userId;
        this.role = role;
    }

    public static User ofClaims(UUID userId, UserRole role) {
        return new User(userId, role);
    }

    /**
     * 수정
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    public void updateStatus(UserStatus status) {
        this.status = status;
    }
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
    public void updateTasteSelected(boolean selected) {
        this.tasteSelected = selected;
    }
    //비밀번호 변경
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    /**
     * 사용자 소프트삭제
     */
//    public void softDelete(UUID deletedBy) {
//        this.status = UserStatus.DELETED;
//        this.deletedAt = LocalDateTime.now();
//        this.deletedBy = deletedBy;
//    }

    public void maskDeletedUserInfo(String addMaskingLetter, String withdrawalNicknamePrefix) {
        this.email = this.email + addMaskingLetter;
        this.nickname = withdrawalNicknamePrefix + addMaskingLetter;
        this.password = this.password + addMaskingLetter;
        this.profileImageUrl = null;
    }

    // 사용자 소프트 삭제 복구(재가입)
    public void reactivate(String email, String password) {
        this.email = email;
        this.password = password;
        this.nickname = null;
        this.reactivatedAt = LocalDateTime.now();
        this.deletedAt = null;
        this.deletedBy = null;
        this.emailVerified = false;
        this.emailVerifiedAt = null;
        this.status = UserStatus.ACTIVE;
        this.tasteSelected = false;
        this.lastLoginAt = null;

    }
}