package com.digniche.muntum.auth.exception;

/**
 * Auth 비즈니스 예외
 */
public class AuthBusinessException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthBusinessException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthBusinessException(AuthErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
    }

    public AuthBusinessException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public AuthBusinessException(AuthErrorCode errorCode, Throwable cause, Object... args) {
        super(String.format(errorCode.getMessage(), args), cause);
        this.errorCode = errorCode;
    }

    public AuthErrorCode getAuthErrorCode() {
        return errorCode;
    }
}
