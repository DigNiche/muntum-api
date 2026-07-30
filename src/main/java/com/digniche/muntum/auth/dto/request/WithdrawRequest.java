package com.digniche.muntum.auth.dto.request;

/**
 * 회원 탈퇴 Request DTO
 *
 * 일반 회원:
 * - password 사용
 *
 * Apple 회원:
 * - token
 * - authorizationCode
 * - nonce 사용
 */
public record WithdrawRequest(

        String password,

        String token,

        String authorizationCode,

        String nonce

) {
}
