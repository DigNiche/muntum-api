package com.digniche.muntum.user.repository;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용작 약관 동의 Repository
 */
// @EntityGraph(attributePaths = {"user"})
public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, UUID> {
    Optional<UserTermsAgreement> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}
