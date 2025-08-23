package com.example.Campung.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 설정 클래스
 * 단일 책임 원칙(SRP)을 준수하여 CORS 정책만 담당
 * 
 * 개발/테스트 환경: 모든 출처 허용
 * 운영 환경: 특정 도메인만 허용하도록 수정 필요
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORS 전역 설정
     * @param registry CORS 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 개발/테스트용: 모든 출처 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // preflight 요청 캐시 시간 (1시간)
    }

    /**
     * CORS 설정 소스 Bean 등록 (Spring Security와 함께 사용할 때 필요)
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 개발/테스트 환경용 설정
        configuration.addAllowedOriginPattern("*"); // 모든 출처 허용
        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 인증 정보 포함 허용
        
        // 운영 환경에서는 아래와 같이 특정 도메인만 허용
        // configuration.setAllowedOrigins(Arrays.asList(
        //     "https://campung.my",
        //     "https://www.campung.my",
        //     "http://localhost:3000" // 프론트엔드 개발 서버
        // ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
