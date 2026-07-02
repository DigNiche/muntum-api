package com.digniche.muntum.program.dto.response;

import com.digniche.muntum.program.entity.ProgramImage;

import java.util.UUID;

/**
 * 프로그램 이미지 Response
 */
public record ProgramImageResponse(
        UUID id,
        String imageUrl,
        int displayOrder,
        UUID programId
) {
    public static ProgramImageResponse from(ProgramImage image) {
        return new ProgramImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getDisplayOrder(),
                image.getProgram().getId()
        );
    }
}
