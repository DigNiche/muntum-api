package com.digniche.muntum.curationlike.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * CurationLike 복합 키 (user_id, curation_id)
 * - equals, hashCode의 비교 대상은 두 필드 값
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CurationLikeId implements Serializable {

    @Column(name="user_id", columnDefinition="BINARY(16)", nullable=false, updatable=false)
    private UUID userId;

    @Column(name="curation_id", columnDefinition="BINARY(16)", nullable=false, updatable=false)
    private UUID curationId;
}
