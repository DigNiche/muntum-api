package com.digniche.muntum.curator.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CuratorApplicationRejectReason {
    INSUFFICIENT_INFO("제출하신 정보가 부족하여 승인되지 않았습니다. 보완 후 재신청해 주세요."),
    GUIDELINE_MISMATCH("작성 가이드라인과 맞지 않아 승인되지 않았습니다. 확인 후 재신청해주세요."),
    PROGRAM_MISMATCH("프로그램 성격과 맞지 않아 승인되지 않았습니다. 내용 확인 후 재신청해주세요.");

    private final String message;
}
