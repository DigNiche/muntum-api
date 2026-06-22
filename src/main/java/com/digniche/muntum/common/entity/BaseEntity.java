package com.digniche.muntum.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 엔티티 기본 정보
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // 생성일, 생성자
    @CreatedDate
    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name="created_by", updatable = false)
    private UUID createdBy;

    // 수정일, 수정자
    @LastModifiedDate
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name="updated_by")
    private UUID updatedBy;
}
