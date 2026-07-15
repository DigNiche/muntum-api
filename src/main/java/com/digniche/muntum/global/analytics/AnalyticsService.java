package com.digniche.muntum.global.analytics;

import com.amplitude.Amplitude;
import com.amplitude.Event;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Amplitude 서버사이드 이벤트 계측 래퍼
 */
@Slf4j
@Component
public class AnalyticsService {

    private final Amplitude amplitude;

    public AnalyticsService(@Value("${amplitude.api-key}") String apiKey) {
        this.amplitude = Amplitude.getInstance();
        this.amplitude.init(apiKey);
    }

    public void track(UUID userId, String eventType, Map<String, Object> properties) {
        if (userId == null) {
            return; // 비로그인 사용자는 v1에서 추적하지 않음
        }
        try {
            Event event = new Event(eventType, userId.toString());
            if (properties != null && !properties.isEmpty()) {
                event.eventProperties = new JSONObject(properties);
            }
            amplitude.logEvent(event);
        } catch (Exception e) {
            log.warn("Amplitude 이벤트 전송 실패: eventType={}, userId={}", eventType, userId, e);
        }
    }
}
