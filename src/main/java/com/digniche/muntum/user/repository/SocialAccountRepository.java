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
}