package com.digniche.muntum.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 약관에 대한 Enum
 */
@Getter
@RequiredArgsConstructor
public enum UserTermsType {

    TERMS_OF_SERVICE(true,  "서비스 이용약관 동의"),
    PRIVACY_POLICY  (true,  "개인정보 수집 및 이용 동의"),
    MARKETING_PUSH  (false, "앱 푸시 알림 수신 동의"),
    MARKETING_EMAIL (false, "이메일 광고·소식 수신 동의"),
    LOCATION_TERMS  (false, "위치기반 서비스 이용약관 동의"),
    THIRD_PARTY_OFFER(false, "제3자 정보 제공 동의");

    private final boolean required;
    private final String description;
}
