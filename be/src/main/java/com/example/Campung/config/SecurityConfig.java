package com.example.campung.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 클래스
 * 단일 책임 원칙(SRP)을 준수하여 보안 설정만 담당
 * 
 * CORS 설정을 CorsConfig와 연동하여 적용
 * Swagger UI 접근 허용
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Spring Security 필터 체인 설정
     * CORS 설정을 CorsConfig Bean과 연동
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정 활성화 (CorsConfig의 corsConfigurationSource Bean 사용)
            .cors(Customizer.withDefaults())
            
            // CSRF 비활성화 (API 서버이므로)
            .csrf(csrf -> csrf.disable())
            
            // 요청 인증 설정
            .authorizeHttpRequests(auth -> auth
                // Swagger UI 관련 경로 허용
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                
                // phpMyAdmin 관련 경로 허용
                .requestMatchers("/phpmyadmin/**").permitAll()
                .requestMatchers("/admin/**").permitAll()
                
                // API 엔드포인트들 허용 (현재는 인증 없이)
                .requestMatchers("/api/**").permitAll()
                
                // 기타 모든 요청 허용 (개발 환경)
                .anyRequest().permitAll()
            );
            
        return http.build();
    }
}
