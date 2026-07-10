package com.digniche.muntum.global.security.jwt.exception;


import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet 진입 전 예외 처리 : 인증되지 않은 요청이 보호된 엔드포인트에 접근했을 때 (401)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.NOT_AUTHENTICATED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(response.getStatus(), errorCode.getCode(), resolveMessage(request))
        ));
    }

    private String resolveMessage(HttpServletRequest request) {
        Object exceptionAttr = request.getAttribute("exception");
        if (exceptionAttr instanceof Exception e && e.getMessage() != null) {
            return e.getMessage();
        }
        return "인증이 필요합니다.";
    }
}
