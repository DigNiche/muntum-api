package com.digniche.muntum.user.repository;

import com.digniche.muntum.user.entity.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용작 약관 동의 Repository
 */
// @EntityGraph(attributePaths = {"user"})
public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, UUID> {
    Optional<UserTermsAgreement> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 사용자의 약관 동의 내역 삭제
     */
    @Modifying
    @Query("DELETE FROM UserTermsAgreement uta WHERE uta.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
