
package com.digniche.muntum.auth.service;

import com.digniche.muntum.auth.dto.request.SignupConsentChoice;
import com.digniche.muntum.auth.dto.request.SignupConsentRequest;
import com.digniche.muntum.global.config.AuditConfig;
import com.digniche.muntum.global.config.AuditorAwareImpl;
import com.digniche.muntum.user.entity.*;
import com.digniche.muntum.user.repository.TermsRepository;
import com.digniche.muntum.user.repository.UserRepository;
import com.digniche.muntum.user.repository.UserTermsAgreementRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("local")
@Import({
        SignupConsentRecorder.class,
        AuditConfig.class,
        SignupConsentRecorderJpaTest.TestAuditorConfig.class
})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=false"
})

class SignupConsentRecorderJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private UserTermsAgreementRepository agreementRepository;

    @Autowired
    private SignupConsentRecorder recorder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void explicitSignupPersistsConsentAndJson() {
        Terms service = ensureActiveTerm(
                UserTermsType.TERMS_OF_SERVICE
        );
        ensureActiveTerm(UserTermsType.PRIVACY_POLICY);
        Terms marketing = ensureActiveTerm(
                UserTermsType.MARKETING_EMAIL
        );

        User user = createTestUser();

        SignupConsentRequest request = new SignupConsentRequest(
                List.of(
                        choice(
                                UserTermsType.TERMS_OF_SERVICE,
                                service.getVersion(), true
                        ),
                        choice(
                                UserTermsType.MARKETING_EMAIL,
                                marketing.getVersion(), true
                        )
                )
        );

        UserTermsAgreement saved =
                recorder.saveExplicitSignup(user, request);

        UUID agreementId = saved.getId();

        // Force Hibernate to send SQL to MySQL.
        entityManager.flush();

        // Clear the first-level cache.
        entityManager.clear();

        // Read the entity back from the database.
        UserTermsAgreement loaded = agreementRepository
                .findById(agreementId)
                .orElseThrow();

        assertNotNull(loaded.getTermsOfServiceAt());
        assertNull(loaded.getPrivacyPolicyAt());

        assertNotNull(loaded.getMarketingEmailAt());
        assertNull(loaded.getMarketingPushAt());

        assertEquals(
                SignupConsentSource.EXPLICIT,
                loaded.getSignupConsentSource()
        );

        assertEquals(
                marketing.getVersion(),
                loaded.getAgreementVersions().get("MARKETING_EMAIL")
        );

        assertEquals(service.getVersion(), loaded.getVersion());

        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getCreatedBy());
        assertNotNull(loaded.getUpdatedAt());
    }

    @Test
    void legacySignupDoesNotInventConsent() {
        Terms service = ensureActiveTerm(
                UserTermsType.TERMS_OF_SERVICE
        );
        ensureActiveTerm(UserTermsType.PRIVACY_POLICY);

        User user = createTestUser();

        UserTermsAgreement saved =
                recorder.saveLegacyUnverifiedSignup(user);

        entityManager.flush();
        entityManager.clear();

        UserTermsAgreement loaded = agreementRepository
                .findById(saved.getId())
                .orElseThrow();

        assertNull(loaded.getTermsOfServiceAt());
        assertNull(loaded.getPrivacyPolicyAt());

        assertEquals(
                SignupConsentSource.LEGACY_UNVERIFIED,
                loaded.getSignupConsentSource()
        );

        assertEquals(service.getVersion(), loaded.getVersion());

        assertTrue(
                loaded.getAgreementVersions() == null
                        || loaded.getAgreementVersions().isEmpty()
        );
    }
    @TestConfiguration
    static class TestAuditorConfig {

        @Bean("auditorAwareImpl")
        AuditorAware<UUID> auditorAware() {
            return () -> Optional.of(
                    UUID.fromString(
                            "00000000-0000-0000-0000-000000000001"
                    )
            );
        }
    }
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedSignupRollsBackUserAndAgreement() {
        String email = uniqueEmail();
        AtomicReference<UUID> userId = new AtomicReference<>();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () ->
                tx.executeWithoutResult(status -> {

                    Terms service = ensureActiveTerm(
                            UserTermsType.TERMS_OF_SERVICE
                    );
                    ensureActiveTerm(
                            UserTermsType.PRIVACY_POLICY
                    );

                    User user = userRepository.save(
                            User.createSocialUser(email, true)
                    );

                    userId.set(user.getId());

                    recorder.saveExplicitSignup(
                            user,
                            new SignupConsentRequest(
                                    List.of(choice(
                                            UserTermsType.TERMS_OF_SERVICE,
                                            service.getVersion(),
                                            true
                                    ))
                            )
                    );

                    // Ensure SQL has reached MySQL
                    // before simulating a failure.
                    entityManager.flush();

                    throw new IllegalStateException(
                            "Simulated signup failure"
                    );
                })
        );

        assertNotNull(userId.get());

        assertTrue(
                userRepository.findByEmail(email).isEmpty(),
                "User must not survive rollback"
        );

        assertTrue(
                agreementRepository
                        .findTopByUserIdOrderByCreatedAtDesc(userId.get())
                        .isEmpty(),
                "Agreement must not survive rollback"
        );
    }

    private User createTestUser() {
        return userRepository.save(
                User.createSocialUser(uniqueEmail(), true)
        );
    }

    private String uniqueEmail() {
        return "consent-test-"
                + UUID.randomUUID()
                + "@example.test";
    }

    private SignupConsentChoice choice(
            UserTermsType type,
            String version,
            boolean agreed
    ) {
        return new SignupConsentChoice(type, version, agreed);
    }

    private Terms ensureActiveTerm(UserTermsType type) {
        return termsRepository
                .findByTypeAndActiveTrueAndDeletedAtIsNull(type)
                .orElseGet(() -> {
                    Terms terms = Terms.builder()
                            .type(type)
                            .version(
                                    "test-" + UUID.randomUUID()
                                            .toString()
                                            .substring(0, 12)
                            )
                            .title("Integration test: " + type.name())
                            .content("Integration test document content")
                            .effectiveAt(
                                    LocalDateTime.now().minusDays(1)
                            )
                            .build();

                    terms.activate();
                    return termsRepository.save(terms);
                });
    }
}
