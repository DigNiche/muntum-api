package com.digniche.muntum.curation.dto.response;

import com.digniche.muntum.curation.entity.CurationImage;

import java.util.UUID;

public record CurationImageResponse(
        UUID id,
        String imageUrl,
        int displayOrder
) {
    public static CurationImageResponse from(CurationImage image) {
        return new CurationImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getDisplayOrder()
        );
    }
}
