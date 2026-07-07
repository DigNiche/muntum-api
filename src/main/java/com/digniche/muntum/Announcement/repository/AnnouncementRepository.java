package com.digniche.muntum.Announcement.repository;

import com.digniche.muntum.Announcement.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * 공지 Repository
 */
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    Optional<Announcement> findByIdAndDeletedAtIsNull(UUID id);
    Page<Announcement> findByDeletedAtIsNull(Pageable pageable);
    Page<Announcement> findAll(Pageable pageable);

    /**
     * 사용자 삭제 시
     * - 생성자, 수정자, 삭제자 System UUID로 채우기
     */

    @Modifying
    @Query("UPDATE Announcement a SET a.createdBy = :systemUuid WHERE a.createdBy = :userId")
    void replaceCreatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Announcement a SET a.updatedBy = :systemUuid WHERE a.updatedBy = :userId")
    void replaceUpdatedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);

    @Modifying
    @Query("UPDATE Announcement a SET a.deletedBy = :systemUuid WHERE a.deletedBy = :userId")
    void replaceDeletedByWithSystem(@Param("userId") UUID userId, @Param("systemUuid") UUID systemUuid);
}
