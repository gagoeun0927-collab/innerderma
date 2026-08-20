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
 * 최소 소유권 검증 (정책 B). {@code /api/users/{userCode}/...} 계열 요청에서 요청자가 경로의
 * userCode 본인인지 확인한다. 요청 헤더 {@code X-User-Code} 가 없으면 401, 경로 userCode 와
 * 다르면 403 을 반환한다. 존재하지 않는 userCode 는 여기서 판단하지 않고 기존 서비스의
 * USER_NOT_FOUND 흐름을 그대로 재사용한다. 로그인/JWT/비밀번호 저장은 이 범위에서 다루지 않는다.
 */
@Component
public class UserOwnershipInterceptor implements HandlerInterceptor {

    public static final String USER_CODE_HEADER = "X-User-Code";
    private static final String USER_PATH_PREFIX = "/api/users/";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS preflight는 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String pathUserCode = extractPathUserCode(request.getRequestURI());
        if (pathUserCode == null) {
            return true;
        }

        // JWT Authorization 헤더가 있으면 JWT 인터셉터가 이미 소유권 검증했으므로 통과
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return true;
        }

        // JWT 없으면 레거시 X-User-Code 헤더로 검증
        String headerUserCode = request.getHeader(USER_CODE_HEADER);
        if (headerUserCode == null || headerUserCode.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!headerUserCode.equals(pathUserCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return true;
    }

    private String extractPathUserCode(String uri) {
        if (uri == null) {
            return null;
        }
        int idx = uri.indexOf(USER_PATH_PREFIX);
        if (idx < 0) {
            return null;
        }
        String rest = uri.substring(idx + USER_PATH_PREFIX.length());
        if (rest.isEmpty()) {
            return null;
        }
        int slash = rest.indexOf('/');
        String segment = slash >= 0 ? rest.substring(0, slash) : rest;
        if (segment.isEmpty()) {
            return null;
        }
        return URLDecoder.decode(segment, StandardCharsets.UTF_8);
    }
}
