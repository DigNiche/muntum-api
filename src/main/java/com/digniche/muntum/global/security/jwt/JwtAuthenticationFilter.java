package com.digniche.muntum.global.security.jwt;

import com.digniche.muntum.auth.service.AccessTokenService;
import com.digniche.muntum.global.ApiResponse;
import com.digniche.muntum.global.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.digniche.muntum.user.repository.UserRepository;
import com.digniche.muntum.user.entity.UserStatus;
import com.digniche.muntum.global.exception.BusinessException;
import com.digniche.muntum.global.exception.ErrorCode;

import java.io.IOException;
import java.util.UUID;

/**
 * SecuritySecurityContext에 인증 정보를 채워주는 필터 - Authorization 헤더 검사하여 진행
 * - DB 조회 없이 JWT claims만으로 인증 구성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    // JWT Access Token 검증 및 활성 사용자 확인
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            Claims claims;
            try {
                // Refresh Token으로 일반 API 인증 불가
                claims = jwtProvider.validAccessToken(token);
            } catch (BusinessException exception) {

                SecurityContextHolder.clearContext();

                ErrorCode errorCode = exception.getErrorCode();

                response.setStatus(errorCode.getStatus().value());
                response.setContentType("application/json;charset=UTF-8");

                response.getWriter().write(
                        objectMapper.writeValueAsString(
                                ApiResponse.fail(errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage())
                        )
                );
                return;
            }

            UUID userId = UUID.fromString(claims.getSubject());

            // 기존 블랙리스트 및 실제 활성 계정 확인
            if (accessTokenService.isWithdrawn(token)
                    || !userRepository.existsByIdAndStatus(
                    userId,
                    UserStatus.ACTIVE
            )) {
                SecurityContextHolder.clearContext();

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResponse.fail(401, "WITHDRAWN_TOKEN", "탈퇴된 사용자의 토큰입니다.")
                ));
                return;
            }
            String userRole = claims.get(JwtProvider.CLAIM_ROLE, String.class);

            SecurityContextHolder.getContext().setAuthentication(createAuthentication(userId, userRole));

        }

        filterChain.doFilter(request, response);
    }

    // 인증된 토큰 생성
    private Authentication createAuthentication(UUID userId, String userRole) {
        UserPrincipal userPrincipal = new UserPrincipal(userId, userRole);
        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
