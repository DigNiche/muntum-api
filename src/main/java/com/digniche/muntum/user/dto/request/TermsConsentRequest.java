package com.digniche.muntum.user.dto.request;

import com.digniche.muntum.user.entity.UserTermsType;
import jakarta.validation.constraints.NotNull;

public record TermsConsentRequest(
        @NotNull UserTermsType termType,
        boolean agreed,
        String version
) {
    // 기존에 인자 두 개로 생성하던 Java 코드와의 호환성 유지
    public TermsConsentRequest(
            UserTermsType termType,
            boolean agreed
    ) {
        this(termType, agreed, null);
    }
}