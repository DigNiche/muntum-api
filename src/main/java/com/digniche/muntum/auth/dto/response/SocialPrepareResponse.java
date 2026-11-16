package com.digniche.muntum.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SocialPrepareResponse(
        Status status,
        AuthenticationResponse authentication,
        String signupTicket,
        Long expiresIn
) {

    public enum Status {
        LOGIN_COMPLETE,
        SIGNUP_REQUIRED
    }

    public SocialPrepareResponse {

        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }

        if (status == Status.LOGIN_COMPLETE) {
            if (authentication == null
                    || signupTicket != null
                    || expiresIn != null) {
                throw new IllegalArgumentException("Invalid login-complete response");
            }
        }

        if (status == Status.SIGNUP_REQUIRED) {
            if (authentication != null
                    || signupTicket == null
                    || signupTicket.isBlank()
                    || expiresIn == null
                    || expiresIn <= 0) {
                throw new IllegalArgumentException("Invalid signup-required response");
            }
        }
    }

    public static SocialPrepareResponse loginComplete(AuthenticationResponse authentication) {
        return new SocialPrepareResponse(Status.LOGIN_COMPLETE, authentication, null, null);
    }

    public static SocialPrepareResponse signupRequired(String signupTicket, long expiresIn) {
        return new SocialPrepareResponse(Status.SIGNUP_REQUIRED, null, signupTicket, expiresIn);
    }
}