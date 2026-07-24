package com.digniche.muntum.global.analytics.event;

import java.util.UUID;

public record SignupCompletedEvent(
        UUID userId
) {
}