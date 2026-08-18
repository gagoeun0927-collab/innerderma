package com.innerderma.common.config;

import com.innerderma.common.security.JwtAuthInterceptor;
import com.innerderma.common.security.UserOwnershipInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JWT 인증 + 소유권 검증 인터셉터를 사용자 스코프 경로에 적용한다.
 * /api/auth/**, /api/facilities, /api/products, /swagger-ui/**, /api-docs/** 등은 대상 외.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final UserOwnershipInterceptor userOwnershipInterceptor;

    public WebConfig(JwtAuthInterceptor jwtAuthInterceptor, UserOwnershipInterceptor userOwnershipInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.userOwnershipInterceptor = userOwnershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/users/**");
        registry.addInterceptor(userOwnershipInterceptor)
                .addPathPatterns("/api/users/**");
    }
}
