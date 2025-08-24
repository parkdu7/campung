package com.example.campung.global.exceptionHandler;

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
@RestControllerAdvice(basePackages = {"com.example.campung.test.controller", "com.example.campung.user.controller", "com.example.campung.content.controller", "com.example.campung.comment.controller", "com.example.campung.contentlike.controller"})  // SpringDoc 2.8.0에서 호환성 문제 해결
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
    
    /**
     * 파일 업로드 크기 초과 예외 처리
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "BAD_REQUEST");
        response.put("message", "파일 크기가 너무 큽니다.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 파일을 찾을 수 없는 경우 예외 처리
     */
    @ExceptionHandler(java.io.FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFoundException(java.io.FileNotFoundException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "NOT_FOUND");
        response.put("message", "파일을 찾을 수 없습니다.");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * 게시글을 찾을 수 없는 경우 예외 처리
     */
    @ExceptionHandler(com.example.campung.global.exception.ContentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContentNotFoundException(com.example.campung.global.exception.ContentNotFoundException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * 권한이 없는 경우 예외 처리
     */
    @ExceptionHandler(com.example.campung.global.exception.UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(com.example.campung.global.exception.UnauthorizedException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "BAD_REQUEST");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 파일 입출력 예외 처리
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(java.io.IOException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "INTERNAL_SERVER_ERROR");
        response.put("message", "파일 처리 중 오류가 발생했습니다.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * PathVariable 변환 실패 예외 처리 (잘못된 경로 매개변수)
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "NOT_FOUND");
        response.put("message", "요청한 리소스를 찾을 수 없습니다.");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * NoHandlerFoundException 처리 (매핑되지 않은 URL)
     */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(org.springframework.web.servlet.NoHandlerFoundException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "NOT_FOUND");
        response.put("message", "요청한 리소스를 찾을 수 없습니다.");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Redis 연결 예외 처리
     */
    @ExceptionHandler({
        org.springframework.data.redis.RedisConnectionFailureException.class,
        org.springframework.data.redis.RedisSystemException.class
    })
    public ResponseEntity<Map<String, Object>> handleRedisException(Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "REDIS_ERROR");
        response.put("message", "Redis 연결 오류가 발생했습니다.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 일반적인 예외 처리 (마지막 fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error_type", "INTERNAL_SERVER_ERROR");
        response.put("message", "서버 내부 오류가 발생했습니다.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
