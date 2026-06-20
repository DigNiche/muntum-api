package com.digniche.muntum.global.security.jwt;

import com.digniche.muntum.global.security.CustomUserDetails;
import com.digniche.muntum.user.entity.UserRole;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                jwtProvider.validateToken(token);

                UUID userId = jwtProvider.getUserId(token);
                UserRole role = jwtProvider.getRole(token);

                SecurityContextHolder.getContext().setAuthentication(createAuthentication(userId, role));
            } catch (RuntimeException e) {
                // JwtProvider가 던지는 구체적인 예외 타입(만료/위변조 등)에 맞춰 catch 절을 좁혀도 된다.
                log.debug("JWT 인증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                request.setAttribute("exception", e);
            }
        }

        // 토큰이 없거나 검증에 실패해도 일단 체인은 계속 진행한다.
        // signup/login처럼 공개 엔드포인트는 인증 없이도 통과해야 하고,
        // 보호된 엔드포인트는 authorizeHttpRequests + AuthenticationEntryPoint가 401로 막아준다.
        filterChain.doFilter(request, response);
    }

    private Authentication createAuthentication(UUID userId, UserRole role) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, role);
        // 3-arg 생성자를 쓰면 즉시 "인증된(authenticated=true)" 토큰이 만들어진다.
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
