package com.digniche.muntum.Announcement.dto.request;

import com.digniche.muntum.Announcement.entity.Announcement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 공지 등록 및 수정 Request
 */
public record AnnouncementRequest(
        @NotBlank(message = "제목을 입력해주세요.") @Size(max = 100) String title,
        @NotBlank(message = "내용을 입력해주세요.") String contents
) {
    public Announcement toEntity() {
        return Announcement.builder()
                .title(this.title())
                .contents(this.contents())
                .build();
    }
}