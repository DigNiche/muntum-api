package com.digniche.muntum.curator.controller;

import com.digniche.muntum.curator.dto.request.CuratorApplicationCreateRequest;
import com.digniche.muntum.curator.dto.request.CuratorApplicationStatusUpdateRequest;
import com.digniche.muntum.curator.dto.response.CuratorApplicationCardResponse;
import com.digniche.muntum.curator.dto.response.CuratorApplicationResponse;
import com.digniche.muntum.curator.entity.CuratorApplicationStatus;
import com.digniche.muntum.curator.service.CuratorApplicationService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 큐레이터 지원을 위한 컨트롤러
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/curator-applications")
public class CuratorApplicationController {

    private final CuratorApplicationService curatorApplicationService;

    /**
     * 큐레이터 지원서 제출
     * - 큐레이터 지원자 : 관람객(AUDIENCE)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("")
    public ResponseEntity<ApiResponse<CuratorApplicationResponse>> submitCuratorApplication(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CuratorApplicationCreateRequest request
    ) {
        CuratorApplicationResponse response = curatorApplicationService.createCuratorApplication(userPrincipal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("지원서가 제출되었습니다.", response));
    }


    /**
     * 큐레이터의 본인 지원 내역 확인(최신 단건)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/latest")
    public ResponseEntity<ApiResponse<CuratorApplicationCardResponse>> viewMyApplication(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CuratorApplicationCardResponse response =
                curatorApplicationService.getLatestApplication(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내 최신 지원 조회에 성공했습니다.", response));
    }


    /**
     * 큐레이터 지원 내역 단건 상세 조회 (큐레이터 본인 / 관리자)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{application_id}")
    public ResponseEntity<ApiResponse<CuratorApplicationResponse>> checkApplication(
            @PathVariable("application_id") UUID applicationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CuratorApplicationResponse response = curatorApplicationService.getApplication(applicationId, userPrincipal.getUserId(), userPrincipal.getUserRole());
        return ResponseEntity.ok(ApiResponse.success("지원 상세 조회에 성공했습니다.", response));
    }


    /**
     * 큐레이터 전체 지원 내역 목록 확인 (큐레이터 본인)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<CuratorApplicationCardResponse>>> viewMyApplicationList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CuratorApplicationCardResponse> response = curatorApplicationService.getMyApplications(userPrincipal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("나의 큐레이터 전체 지원 목록 조회에 성공했습니다.", response));
    }


    /**
     * 전체 큐레이터 지원 내역 목록 확인
     * - 상태 필터 선택적
     */
    @PreAuthorize("hasAnyRole('MANAGER')")
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<PageResponse<CuratorApplicationCardResponse>>> viewAllApplicationList(
            @RequestParam(required = false) CuratorApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CuratorApplicationCardResponse> response = curatorApplicationService.getAllApplications(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("지원 전체 목록 조회에 성공했습니다.", response));
    }

    /**
     * 큐레이터 지원 심사 -> 승인 및 반려
     */
    @PreAuthorize("hasAnyRole('MANAGER')")
    @PatchMapping("/{application_id}/status")
    public ResponseEntity<ApiResponse<CuratorApplicationResponse>> evaluateApplication(
            @PathVariable("application_id") UUID applicationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CuratorApplicationStatusUpdateRequest request
    ) {
        CuratorApplicationResponse response = curatorApplicationService.evaluateApplication(
                applicationId, userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("지원 상태가 변경되었습니다.", response));
    }
}