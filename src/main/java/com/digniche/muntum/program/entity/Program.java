package com.digniche.muntum.program.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.time.LocalDateTime;
/**
 * 프로그램 엔티티
 */

@Entity
@Table(name= "programs")
@Getter
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
public class Program extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING) //Enum 이름을 DB 문자열로 저장
    @Column(name = "type", nullable = false, length = 20)
    private ProgramType programType;

    @Column(name = "tagline", nullable = false, length = 255)
    private String tagline;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String curation;

    @Column(name = "is_reserved", nullable = false)
    private boolean reserved = false;

    @Column(name = "is_free", nullable = false)
    private boolean free = true;

    @Column(length = 255)
    private String price;

    @Column(name = "venue_name", nullable = false, length = 100)
    private String venueName;

    @Column(name = "venue_meta", length = 255)
    private String venueMeta;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "operating_period_meta", length = 255)
    private String operatingPeriodMeta;

    @Column(name = "operating_hours", length = 500)
    private String operatingHours;

    @Column(name = "operating_hours_meta", length = 255)
    private String operatingHoursMeta;

    @Column(name = "inquiry_contact", length = 255)
    private String inquiryContact;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(
            name = "deleted_by",
            columnDefinition = "BINARY(16)"
    )
    private UUID deletedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramStatus status = ProgramStatus.ACTIVE;

    @Builder
    public Program(
            String title,
            ProgramType  programType,
            String tagline,
            String curation,
            Boolean reserved,
            Boolean free,
            String price,
            String venueName,
            String venueMeta,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String officialUrl,
            LocalDate startDate,
            LocalDate endDate,
            String operatingPeriodMeta,
            String operatingHours,
            String operatingHoursMeta,
            String inquiryContact
    ) {
        this.title = title;
        this.programType = programType;
        this.tagline = tagline;
        this.curation = curation;
        this.reserved = reserved;  // @NotNull + @Valid로 보장됨
        this.free = free; // 나머지 String/날짜 필드는 this.x = x 그대로
        this.price = price;
        this.venueName = venueName;
        this.venueMeta = venueMeta;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.officialUrl = officialUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.operatingPeriodMeta = operatingPeriodMeta;
        this.operatingHours = operatingHours;
        this.operatingHoursMeta = operatingHoursMeta;
        this.inquiryContact = inquiryContact;
        this.viewCount = 0;
        this.status = ProgramStatus.ACTIVE;
    }

    /**
     * 논리 삭제 처리
     */
    public void softDelete(UUID deletedBy) {
        this.status = ProgramStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    /**
     * 프로그램 정보 수정 (PUT - 전체 교체)
     */
    public void update(
            String title,
            ProgramType programType,
            String tagline,
            String curation,
            Boolean reserved,
            Boolean free,
            String price,
            String venueName,
            String venueMeta,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String officialUrl,
            LocalDate startDate,
            LocalDate endDate,
            String operatingPeriodMeta,
            String operatingHours,
            String operatingHoursMeta,
            String inquiryContact
    ) {
        this.title = title;
        this.programType = programType;
        this.tagline = tagline;
        this.curation = curation;
        this.reserved = reserved != null ? reserved : this.reserved;
        this.free = free != null ? free : this.free;
        this.price = price;
        this.venueName = venueName;
        this.venueMeta = venueMeta;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.officialUrl = officialUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.operatingPeriodMeta = operatingPeriodMeta;
        this.operatingHours = operatingHours;
        this.operatingHoursMeta = operatingHoursMeta;
        this.inquiryContact = inquiryContact;
    }
    public void increaseViewCount() {
        this.viewCount++;
    }

    public void updateStatus(ProgramStatus status) {
        this.status = status;
    }
}

