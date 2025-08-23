package com.example.Campung.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS(Cross-Origin Resource Sharing) 설정 클래스
 * 단일 책임 원칙(SRP)을 준수하여 CORS 정책만 담당
 * 
 * 개발 환경: localhost 기반 origins만 허용
 * 운영 환경: 특정 도메인만 허용
 * 
 * allowCredentials(true) 사용 시 wildcard(*) 사용 불가
 * 보안을 위해 구체적인 origins만 명시
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORS 전역 설정
     * allowCredentials(true) 사용 시 구체적인 origins만 허용
     * @param registry CORS 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(getAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // preflight 요청 캐시 시간 (1시간)
    }

    /**
     * 허용된 Origins 목록 반환
     * 개발/운영 환경에 따라 다른 origins 허용
     * @return 허용된 origins 리스트
     */
    private List<String> getAllowedOrigins() {
        return Arrays.asList(
            // 개발 환경 - 일반적인 프론트엔드 개발 서버 포트들
            "http://localhost:3000",   // React 기본 포트
            "http://localhost:3001",   // React 추가 포트
            "http://localhost:5173",   // Vite 기본 포트
            "http://localhost:5174",   // Vite 추가 포트
            "http://localhost:8080",   // 일반적인 개발 포트
            "http://localhost:8081",   // 백엔드 개발 포트
            "http://127.0.0.1:3000",   // 로컬 IP 기반
            "http://127.0.0.1:5173",   
            
            // 운영 환경
            "https://campung.my",
            "https://www.campung.my",
            "https://api.campung.my"
        );
    }

    /**
     * CORS 설정 소스 Bean 등록 (Spring Security와 함께 사용할 때 필요)
     * allowCredentials(true) 사용 시 구체적인 origins만 허용
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 구체적인 origins 설정 (allowCredentials와 호환)
        configuration.setAllowedOrigins(getAllowedOrigins());
        
        // 허용할 HTTP 메서드들
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 허용할 헤더들
        configuration.setAllowedHeaders(Arrays.asList(
            "*" // 모든 헤더 허용
        ));
        
        // 브라우저에 노출할 헤더들
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept"
        ));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
