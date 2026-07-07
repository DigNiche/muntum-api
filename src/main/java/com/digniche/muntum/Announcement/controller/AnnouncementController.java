package com.digniche.muntum.Announcement.controller;

import com.digniche.muntum.Announcement.dto.request.AnnouncementRequest;
import com.digniche.muntum.Announcement.dto.response.AnnouncementForManagerResponse;
import com.digniche.muntum.Announcement.dto.response.AnnouncementResponse;
import com.digniche.muntum.Announcement.service.AnnouncementService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // 공지 작성
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> registerAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.createAnnouncement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항이 등록되었습니다.", response));
    }

    // 공지 수정
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{announcement_id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> rewriteAnnouncement(
            @PathVariable("announcement_id") UUID announcmentId,
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.updateAnnouncement(announcmentId, request);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 수정되었습니다.", response));
    }

    // 공지 삭제
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{announcement_id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable("announcement_id") UUID announcmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        announcementService.softDeleteAnnouncement(announcmentId, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("공지사항이 삭제되었습니다.", null));
    }

    // 공지 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AnnouncementResponse>>> listAnnouncements(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("공지사항 목록이 조회되었습니다", announcementService.getAnnouncements(pageable)));
    }

    // 삭제된 공지까지 모두 조회
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<PageResponse<AnnouncementForManagerResponse>>> listAnnouncementsForManager(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("공지사항 목록이 삭제된 것까지 모두 조회되었습니다.", announcementService.getAnnouncementsForManager(pageable)));
    }

    // 공지 단건 조회
    @GetMapping("/{announcement_id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(
            @PathVariable("announcement_id") UUID announcmentId) {
        return ResponseEntity.ok(ApiResponse.success("공지사항이 조회되었습니다.", announcementService.getAnnouncement(announcmentId)));
    }
}