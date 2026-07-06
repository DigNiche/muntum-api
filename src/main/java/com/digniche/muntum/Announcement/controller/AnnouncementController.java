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

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.createAnnouncement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항이 등록되었습니다.", response));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable UUID id,
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.updateAnnouncement(id, request);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 수정되었습니다.", response));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        announcementService.deleteAnnouncement(id, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("공지사항이 삭제되었습니다.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AnnouncementResponse>>> getAnnouncements(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(null, announcementService.getAnnouncements(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(null, announcementService.getAnnouncement(id)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<PageResponse<AnnouncementForManagerResponse>>> getAnnouncementsForManager(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(null, announcementService.getAnnouncementsForManager(pageable)));
    }
}