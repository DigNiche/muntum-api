package com.digniche.muntum.Announcement.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 공지사항 엔티티
 */
@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseEntity {

    /**
     * 공지 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    /**
     * 공지 제목
     */
    @Column(
            name = "title",
            nullable = false,
            length = 100
    )
    private String title;

    /**
     * 공지 내용
     */
    @Column(
            name = "contents",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String contents;

    @Builder
    public Announcement(
            String title,
            String contents
    ) {
        this.title = title;
        this.contents = contents;
    }

    /**
     * 공지 수정
     */
    public void update(
            String title,
            String contents
    ) {
        this.title = title;
        this.contents = contents;
    }
}