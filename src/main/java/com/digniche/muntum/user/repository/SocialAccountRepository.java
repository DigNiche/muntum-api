package com.digniche.muntum.user.repository;

import com.digniche.muntum.user.entity.SocialAccount;
import com.digniche.muntum.user.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository
        extends JpaRepository<SocialAccount, UUID> {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    boolean existsByUserIdAndProvider(
            UUID userId,
            SocialProvider provider
    );
    //탈퇴하는 사용자가 일반 or Social 회원인지 확인
    Optional<SocialAccount> findByUserId(
            UUID userId
    );
    // Apple 탈퇴 후 social_accounts 행 먼저 삭제하기 위해 필요
    void deleteAllByUserId(
            UUID userId
    );
}