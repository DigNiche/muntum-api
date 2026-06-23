package com.digniche.muntum.auth.dto.request;

import com.digniche.muntum.user.entity.User;
import com.digniche.muntum.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 Req DTO
 */
public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        UserRole role
) {

    public SignUpRequest {
        if (role == null) role = UserRole.AUDIENCE;
    }

    // TODO: Presentation 계층이 Domain 침범하지 않도록 추후 수정 예정
    public User toEntity(String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build();
    }

}
