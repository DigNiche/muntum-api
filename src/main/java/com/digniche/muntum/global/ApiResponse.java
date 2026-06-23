package com.digniche.muntum.global;

import lombok.Builder;
import lombok.Getter;

/**
 * 일관된 API 응답 형식
 */
@Getter
@Builder
public class ApiResponse<T> {

    private int status;
    private String message;
    private String error;
    private T data;

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return ApiResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> fail(
            int status,
            String error,
            String message
    ) {
        return ApiResponse.<Void>builder()
                .status(status)
                .error(error)
                .message(message)
                .build();
    }
}
