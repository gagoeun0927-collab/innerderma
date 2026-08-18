package com.innerderma.common.config;

import com.innerderma.common.security.UserOwnershipInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 최소 소유권 검증 인터셉터를 사용자 스코프 경로에만 적용한다.
 * {@code /api/facilities}, {@code /api/products}, {@code /api/innerderma/health} 등
 * 공용 조회 API 는 대상 경로에 포함되지 않아 자연히 검증에서 제외된다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserOwnershipInterceptor userOwnershipInterceptor;

    public WebConfig(UserOwnershipInterceptor userOwnershipInterceptor) {
        this.userOwnershipInterceptor = userOwnershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userOwnershipInterceptor).addPathPatterns("/api/users/**");
    }
}
