package com.digniche.muntum.Announcement.dto.response;

import com.digniche.muntum.Announcement.entity.Announcement;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 공지 등록 및 수정 및 조회 Response
 */
public record AnnouncementForManagerResponse(
        UUID id, String title,
        String contents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static AnnouncementForManagerResponse from(Announcement announcement) {
        return new AnnouncementForManagerResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContents(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                announcement.getDeletedAt()
        );
    }
}
