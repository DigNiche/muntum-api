package com.digniche.muntum.Announcement.service;

import com.digniche.muntum.Announcement.dto.request.AnnouncementRequest;
import com.digniche.muntum.Announcement.dto.response.AnnouncementForManagerResponse;
import com.digniche.muntum.Announcement.dto.response.AnnouncementResponse;
import com.digniche.muntum.Announcement.entity.Announcement;
import com.digniche.muntum.Announcement.repository.AnnouncementRepository;
import com.digniche.muntum.global.PageResponse;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 공지 서비스
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    // 공지사항 등록
    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = request.toEntity();
        Announcement res = announcementRepository.save(announcement);
        return AnnouncementResponse.from(res);
    }

    // 공지사항 수정
    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID announcementId, AnnouncementRequest request) {
        Announcement announcement = findAnnouncement(announcementId);
        announcement.update(request.title(), request.contents());
        return AnnouncementResponse.from(announcement);
    }

    // 공지사항 삭제
    @Transactional
    public void softDeleteAnnouncement(UUID announcementId, UUID deletedBy) {
        Announcement announcement = findAnnouncement(announcementId);
        announcement.softDelete(deletedBy);
    }

    // 사용자용 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> getAnnouncements(Pageable pageable) {
        return PageResponse.from(
                announcementRepository.findByDeletedAtIsNull(pageable)
                        .map(AnnouncementResponse::from)
        );
    }

    // 관리자용 목록 조회 - deleted 된 것까지 조회
    @Transactional(readOnly = true)
    public PageResponse<AnnouncementForManagerResponse> getAnnouncementsForManager(Pageable pageable) {
        return PageResponse.from(
                announcementRepository.findAll(pageable)
                        .map(AnnouncementForManagerResponse::from)
        );
    }

    // 공지사항 단건 조회
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncement(UUID announcementId) {
        return AnnouncementResponse.from(findAnnouncement(announcementId));
    }

    /**
     * DB 조회
     */
    private Announcement findAnnouncement(UUID announcementId) {
        return announcementRepository.findByIdAndDeletedAtIsNull(announcementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
    }
}
