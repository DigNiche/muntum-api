package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.curator.entity.CuratorApplication;

/**
 * 큐레이터 지원서 상세 조회 응답 DTO
 */
public record ApplicationContentResponse(
        String programName,
        String tagline,
        String curation
) {
    public static ApplicationContentResponse from(CuratorApplication application) {
        return new ApplicationContentResponse(
                application.getProgramName(),
                application.getTagline(),
                application.getCuration()
        );
    }
}