package com.innerderma.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI innerDermaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InnerDerma API")
                        .description("InnerDerma AI 피부 사후관리 서비스 백엔드 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("InnerDerma Team")));
    }
}
