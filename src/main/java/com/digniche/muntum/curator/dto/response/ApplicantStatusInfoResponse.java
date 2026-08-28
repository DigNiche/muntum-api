package com.digniche.muntum.curator.dto.response;

import com.digniche.muntum.curator.entity.CuratorApplication;
import com.digniche.muntum.curator.entity.CuratorApplicationRejectReason;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

/**
 * 지원자 상태 정보 응답 DTO
 * - 승인 상태 시, rejectReason, rejectReasonMessage 나타나지 않음
 * - 반려 상태 시, rejectReason, rejectReasonMessage 나타남
 * - 대기 상태 시, rejectReason, rejectReasonMessage, reviewedAt 나타나지 않음
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicantStatusInfoResponse(
        CuratorApplicationStatus status,
        CuratorApplicationRejectReason rejectReason,
        String rejectReasonMessage,
        LocalDateTime reviewedAt
) {
    public static ApplicantStatusInfoResponse from(CuratorApplication application) {
        return new ApplicantStatusInfoResponse(
                application.getStatus(),
                application.getRejectReason(),
                (application.getRejectReason() != null) ? application.getRejectReason().getMessage() : null,
                application.getReviewedAt()
        );
    }
}
