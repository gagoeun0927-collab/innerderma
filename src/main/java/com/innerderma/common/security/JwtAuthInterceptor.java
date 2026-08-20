package com.innerderma.common.security;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * JWT 기반 소유권 검증 인터셉터. /api/users/{userCode}/... 요청에서
 * Authorization: Bearer 토큰의 subject(userCode)가 경로의 userCode와 일치하는지 검증.
 * 토큰 없으면 401, 불일치면 403.
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_PATH_PREFIX = "/api/users/";

    private final JwtProvider jwtProvider;

    public JwtAuthInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS preflight(OPTIONS)는 인증 없이 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String pathUserCode = extractPathUserCode(request.getRequestURI());
        if (pathUserCode == null) {
            return true; // /api/users/** 가 아닌 경로는 통과
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtProvider.isValid(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String tokenUserCode = jwtProvider.extractUserCode(token);
        if (!tokenUserCode.equals(pathUserCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return true;
    }

    private String extractPathUserCode(String uri) {
        if (uri == null) return null;
        int idx = uri.indexOf(USER_PATH_PREFIX);
        if (idx < 0) return null;
        String rest = uri.substring(idx + USER_PATH_PREFIX.length());
        if (rest.isEmpty()) return null;
        int slash = rest.indexOf('/');
        String segment = slash >= 0 ? rest.substring(0, slash) : rest;
        if (segment.isEmpty()) return null;
        return URLDecoder.decode(segment, StandardCharsets.UTF_8);
    }
}
