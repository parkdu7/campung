package com.example.Campung.Global.ExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리를 담당하는 핸들러
 * 단일 책임 원칙(SRP)을 준수하여 예외 처리만 담당
 * 중앙집중식 예외 처리를 통해 일관된 에러 응답 제공
 * Spring Boot 3.x 호환 - 특정 패키지만 대상으로 제한
 * @RestControllerAdvice 사용으로 SpringDoc 호환성 개선
 */
@RestControllerAdvice(basePackages = {"com.example.Campung.Test.controller", "com.example.Campung.User.controller"})  // SpringDoc 2.8.0에서 호환성 문제 해결
public class GlobalExceptionHandler {
    
    /**
     * IllegalArgumentException 처리
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "INVALID_ARGUMENT");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * RuntimeException 처리
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "RUNTIME_ERROR");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 데이터베이스 관련 예외 처리
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(org.springframework.dao.DataAccessException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "DATABASE_ERROR");
        response.put("message", "데이터베이스 오류가 발생했습니다.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 유효성 검증 예외 처리
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "VALIDATION_ERROR");
        response.put("message", "입력값 유효성 검증에 실패했습니다.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    // Exception.class 핸들러 제거 - Swagger 내부 예외를 방해하지 않도록
}
