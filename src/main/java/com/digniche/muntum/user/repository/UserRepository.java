package com.digniche.muntum.user.repository;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.email LIKE CONCAT(:email, :maskingLetterPrefix, '%') ESCAPE '\\'")
    Optional<User> findWithdrawnUserForReactivation(@Param("email") String email, @Param("status") UserStatus status, @Param("maskingLetterPrefix") String MASKING_LETTER_PREFIX);

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
}
