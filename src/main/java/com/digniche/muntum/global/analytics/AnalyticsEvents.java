package com.digniche.muntum.global.analytics;

import com.digniche.muntum.program.dto.request.ProgramFilterChip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AnalyticsEvents {

    private final AnalyticsService analyticsService;

    public void programSearched(UUID userId, String query, List<String> keywordNames,
                                ProgramFilterChip chip, long resultCount) {
        Map<String, Object> props = new HashMap<>();
        props.put("query", query);
        props.put("keywordNames", keywordNames);
        props.put("chip", chip);
        props.put("resultCount", resultCount);
        props.put("isZeroResult", resultCount == 0);
        analyticsService.track(userId, "program_searched", props);
    }

    public void programDetailViewed(UUID userId, UUID programId) {
        analyticsService.track(userId, "program_detail_viewed", Map.of("programId", programId));
    }

    public void programScrapped(UUID userId, UUID programId) {
        analyticsService.track(userId, "program_scrapped", Map.of("programId", programId));
    }

    public void scrapListViewed(UUID userId) {
        analyticsService.track(userId, "scrap_list_viewed", Map.of());
    }

    public void spotSuggested(UUID userId) {
        analyticsService.track(userId, "spot_suggested", Map.of());
    }

    public void tasteKeywordsSelected(UUID userId, List<String> keywordNames) {
        analyticsService.track(userId, "taste_keywords_selected", Map.of("keywordNames", keywordNames));
    }
    public void signupCompleted(UUID userId, boolean isReactivation) {
        analyticsService.track(userId, "signup_completed", Map.of("is_reactivation", isReactivation));
    }

    public void onboardingCompleted(UUID userId, int keywordCount) {
        analyticsService.track(userId, "onboarding_completed", Map.of("keyword_count", keywordCount));
    }
}
