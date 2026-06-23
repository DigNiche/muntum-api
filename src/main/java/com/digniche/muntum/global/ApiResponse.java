package com.digniche.muntum.global;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 일관된 API 응답 형식
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer status;
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
