package com.digniche.muntum.curation.entity;

import com.digniche.muntum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "program_curation_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_curation_images_order",
                        columnNames = {
                                "program_curation_id",
                                "display_order"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_program_curation_images_curation",
                        columnList = "program_curation_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurationImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "BINARY(16)",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "program_curation_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_program_curation_images_curation"
            )
    )
    private Curation Curation;

    @Column(
            name = "image_url",
            nullable = false,
            length = 500
    )
    private String imageUrl;

    @Column(
            name = "display_order",
            nullable = false
    )
    private int displayOrder;

    @Builder
    public CurationImage(
            Curation Curation,
            String imageUrl,
            int displayOrder
    ) {
        this.Curation = Curation;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}