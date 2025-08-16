package com.example.Campung.Test.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 홈 페이지 관련 API를 제공하는 컨트롤러
 * 단일 책임 원칙(SRP)을 준수하여 기본 페이지 응답만 담당
 */
@RestController
public class HomeController {
    
    /**
     * 기본 홈 페이지 API
     * @return 환영 메시지와 API 정보
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "🎪 캠펑 프로젝트에 오신 것을 환영합니다!");
        response.put("version", "1.0.0");
        response.put("description", "MariaDB, Redis, PHPMyAdmin 통합 테스트 API");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("모든 서비스 테스트", "/api/test/all");
        endpoints.put("MariaDB 테스트", "/api/test/database");
        endpoints.put("Redis 테스트", "/api/test/redis");
        endpoints.put("테스트 데이터 조회", "/api/test/data");
        endpoints.put("PHPMyAdmin", "http://localhost:9012");
        
        response.put("available_endpoints", endpoints);
        
        return ResponseEntity.ok(response);
    }
}
