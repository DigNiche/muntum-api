package com.digniche.muntum.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 닉네임 설정 Request
 * - 생성 및 수정
 */
public record UpdateNicknameRequest(
    @NotBlank @Size(min=1, max=50) String nickname
) {}
