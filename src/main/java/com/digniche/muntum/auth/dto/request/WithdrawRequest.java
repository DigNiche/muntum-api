package com.digniche.muntum.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 회원 탈퇴 Request DTO
 */
public record WithdrawRequest (
    @NotBlank String password
){ }
