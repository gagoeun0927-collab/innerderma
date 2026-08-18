package com.innerderma.common.config;

import com.innerderma.common.security.JwtAuthInterceptor;
import com.innerderma.common.security.UserOwnershipInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JWT 인증 + 소유권 검증 인터셉터를 사용자 스코프 경로에 적용한다.
 * CORS: 프론트엔드 연동용. 기본값 "*" (시연용), 프로덕션에서는 CORS_ALLOWED_ORIGINS 환경변수로 제한.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final UserOwnershipInterceptor userOwnershipInterceptor;

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    public WebConfig(JwtAuthInterceptor jwtAuthInterceptor, UserOwnershipInterceptor userOwnershipInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.userOwnershipInterceptor = userOwnershipInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(!"*".equals(allowedOrigins.trim()))
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/users/**");
        registry.addInterceptor(userOwnershipInterceptor)
                .addPathPatterns("/api/users/**");
    }
}
