package com.digniche.muntum.user.entity;

// 변경: BaseEntity 대신 삭제 감사 필드까지 가진 SoftDeleteEntity 사용
import com.digniche.muntum.common.entity.SoftDeleteEntity;
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

public class User extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "email",
            nullable = false,
            unique = true
    )
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(
            name = "nickname",
            unique = true,
            length = 50
    )
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 20
    )
    private UserRole role = UserRole.AUDIENCE;

    @Column(name = "taste_selected", nullable = false)
    private boolean tasteSelected = false;

    // 변경: DB 컬럼명을 명확하게 지정
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    /**
     * 사용자 엔티티 생성
     *
     * @param email    사용자 이메일
     * @param password 암호화된 비밀번호
     * @param role     사용자 역할
     */
    @Builder
    public User(
            String email,
            String password,
            UserRole role
    ) {
        this.email = email;
        this.password = password;

        // 변경: Builder에서 role을 생략해도 null로 덮어쓰지 않도록 방어
        this.role = role != null
                ? role
                : UserRole.AUDIENCE;

        // 신규 사용자는 항상 ACTIVE 상태로 생성
        this.status = UserStatus.ACTIVE;

        // 생성 시 기본값을 명확하게 보장
        this.emailVerified = false;
        this.tasteSelected = false;
    }

    /**
     * JWT Claims 등에서 사용자 식별 정보만 임시로 구성할 때 사용
     */
    private User(UUID userId, UserRole role) {
        this.id = userId;
        this.role = role;
    }

    public static User ofClaims(UUID userId, UserRole role) {
        return new User(userId, role);
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    /**
     * 사용자 소프트 삭제
     *
     * 변경:
     * UserStatus를 DELETED로 변경한 뒤,
     * 부모 클래스에서 deletedAt과 deletedBy를 함께 기록
     */
    @Override
    public void softDelete(UUID deletedBy) {
        this.status = UserStatus.DELETED;
        super.softDelete(deletedBy);
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateTasteSelected() {
        this.tasteSelected = true;
    }

    /**
     * 사용자 ID 조회
     *
     * @Getter로 getId()도 자동 생성되지만,
     * 기존 인증 코드에서 getUserId()를 사용하고 있을 수 있으므로 유지
     */
    public UUID getUserId() {
        return this.id;
    }
}