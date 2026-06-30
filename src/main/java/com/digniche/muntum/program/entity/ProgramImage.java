package com.digniche.muntum.program.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "program_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_images_program_order",
                        columnNames = {"program_id", "display_order"} //이미지 순서 안겹치게
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false) //나중에 DTO 검증에서 displayOrder >= 1을 보장
    private int displayOrder;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(
            name = "deleted_by",
            columnDefinition = "BINARY(16)"
    )
    private UUID deletedBy;

    @Builder
    public ProgramImage(
            Program program,
            String imageUrl,
            int displayOrder
    ) {
        this.program = program;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    // 소프트 삭제
    public void softDelete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }


    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}