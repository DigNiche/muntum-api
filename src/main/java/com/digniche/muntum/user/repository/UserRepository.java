package com.digniche.muntum.user.repository;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);


    /** SELECT count(u) > 0 FROM User u WHERE u.email = ?1 AND u.status <> ?2
     * - 이메일이 존재하고 + 해당 status가 아닌(Not) 사용자 존재 여부
     * @param email
     * @param status
     * @return
     */
    boolean existsByEmailAndStatusNot(String email, UserStatus status);
    boolean existsByNicknameAndIdNot(String nickname, UUID id);

    void deleteByStatusAndDeletedAtBefore(UserStatus userStatus, LocalDateTime ago);

    /**
     * 관리자 사용자 관리 - 전체 목록 조회
     */
    Page<User> findAllByStatusNot(UserStatus status, Pageable pageable);

    /**
     * 관리자 사용자 관리 - 닉네임 또는 이메일 검색
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.status <> :excludedStatus
        AND (LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<User> searchByNicknameOrEmail(
            @Param("search") String search,
            @Param("excludedStatus") UserStatus excludedStatus,
            Pageable pageable
    );

    boolean existsByIdAndStatus(UUID id, UserStatus status);


    /**
     * 사용자 행 배타 잠금 조회
     * - Lock 순서: 사용자 > 큐레이션 > 좋아요
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);


    /**
     * 사용자 행 공유 잠금(행 개수만 세고, 정보 읽지는 않음)
     * - 사용자 행이 있으면 1, 없으면 0 반환
     * - FOR SHARE를 그대로 드러내고, 결과를 숫자 하나로 받으려고 네이티브 쿼리 사용
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId FOR SHARE", nativeQuery = true)
    long countByIdForShare(@Param("userId") UUID userId);


    /**
     * 관리자 탈퇴 시, 그 관리자가 수정한 다른 회원 행의 수정자를 Withdrawn UUID로 채우기
     * - idx_users_updated_by로 해당 행만 잠근다
     */
    @Modifying
    @Query("UPDATE User u SET u.updatedBy = :withdrawnUuid WHERE u.updatedBy = :userId")
    int replaceUpdatedByWith(@Param("userId") UUID userId, @Param("withdrawnUuid") UUID withdrawnUuid);
}
