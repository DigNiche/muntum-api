package com.digniche.muntum.Announcement.repository;

import com.digniche.muntum.Announcement.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 공지 Repository
 */
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    Optional<Announcement> findByIdAndDeletedAtIsNull(UUID id);
    Page<Announcement> findByDeletedAtIsNull(Pageable pageable);
    Page<Announcement> findAll(Pageable pageable);
}
