package com.example.Campung.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 설정 클래스 (간소화)
 * 단일 책임 원칙(SRP)을 준수하여 API 문서화 설정만 담당
 */
@Configuration
public class SwaggerConfig {
    
    /**
     * OpenAPI 3.0 스펙 설정 (기본 설정)
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI campungOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎪 Campung API")
                        .description("MariaDB, Redis, PHPMyAdmin 통합 테스트 및 관리 API")
                        .version("1.0.0"));
    }
}
