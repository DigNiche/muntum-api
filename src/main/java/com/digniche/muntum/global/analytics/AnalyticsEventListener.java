package com.digniche.muntum.global.analytics;

import com.digniche.muntum.global.analytics.event.OnboardingCompletedEvent;
import com.digniche.muntum.global.analytics.event.SignupCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private final AnalyticsEvents analyticsEvents;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSignupCompleted(SignupCompletedEvent event) {
        analyticsEvents.signupCompleted(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOnboardingCompleted(OnboardingCompletedEvent event) {
        analyticsEvents.onboardingCompleted(event.userId(), event.keywordCount());
    }
}