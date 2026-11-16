package com.digniche.muntum.user.service;

import com.digniche.muntum.user.entity.Terms;

import java.time.LocalDateTime;

public final class TermsAvailability {

    private TermsAvailability() {
    }

    public static boolean canCollectConsent(
            Terms terms,
            LocalDateTime now
    ) {
        return terms != null
                && terms.isActive()
                && terms.getDeletedAt() == null
                && terms.getEffectiveAt() != null
                && !terms.getEffectiveAt().isAfter(now)
                && terms.getTitle() != null
                && !terms.getTitle().isBlank()
                && terms.getContent() != null
                && !terms.getContent().isBlank()
                && terms.getVersion() != null
                && !terms.getVersion().isBlank();
    }
}