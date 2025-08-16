package com.example.Campung.Test.controller;

import com.example.Campung.Test.entity.TestEntity;
import com.example.Campung.Test.service.DatabaseService;
import com.example.Campung.Test.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트 관련 API를 제공하는 컨트롤러
 * 단일 책임 원칙(SRP)을 준수하여 HTTP 요청 처리만 담당
 * 의존성 역전 원칙(DIP)을 준수하여 추상화(Service Interface)에 의존
 */
@Tag(name = "🧪 테스트 API", description = "MariaDB, Redis, PHPMyAdmin 연결 테스트 및 데이터 관리 API")
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private final DatabaseService databaseService;
    private final RedisService redisService;
    
    /**
     * 의존성 주입을 통한 생성자
     * 의존성 주입 원칙을 준수하여 생성자를 통해 의존성을 주입받음
     */
    @Autowired
    public TestController(DatabaseService databaseService, RedisService redisService) {
        this.databaseService = databaseService;
        this.redisService = redisService;
    }
    
    /**
     * MariaDB 연결 상태 확인 API
     * @return MariaDB 연결 상태 메시지
     * @throws Exception 연결 실패 시
     */
    @Operation(summary = "🗄️ MariaDB 연결 테스트", description = "MariaDB 데이터베이스 연결 상태를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "연결 성공"),
            @ApiResponse(responseCode = "500", description = "연결 실패")
    })
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> testDatabase() throws Exception {
        String result = databaseService.checkDatabaseConnection();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", result);
        response.put("service", "MariaDB");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Redis 연결 상태 확인 API
     * @return Redis 연결 상태 메시지
     * @throws Exception 연결 실패 시
     */
    @Operation(summary = "🔴 Redis 연결 테스트", description = "Redis 서버 연결 상태를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "연결 성공"),
            @ApiResponse(responseCode = "500", description = "연결 실패")
    })
    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> testRedis() throws Exception {
        String result = redisService.checkRedisConnection();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", result);
        response.put("service", "Redis");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 서비스 연결 상태 확인 API
     * @return 모든 서비스 연결 상태
     */
    @Operation(summary = "🎯 전체 서비스 연결 테스트", description = "MariaDB, Redis, PHPMyAdmin 모든 서비스의 연결 상태를 한 번에 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "테스트 완료 (일부 서비스 실패 가능)")
    })
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> testAllConnections() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> results = new HashMap<>();
        
        // MariaDB 테스트
        try {
            String dbResult = databaseService.checkDatabaseConnection();
            results.put("mariadb", Map.of("status", "success", "message", dbResult));
        } catch (Exception e) {
            results.put("mariadb", Map.of("status", "error", "message", "❌ MariaDB 연결 실패: " + e.getMessage()));
        }
        
        // Redis 테스트
        try {
            String redisResult = redisService.checkRedisConnection();
            results.put("redis", Map.of("status", "success", "message", redisResult));
        } catch (Exception e) {
            results.put("redis", Map.of("status", "error", "message", "❌ Redis 연결 실패: " + e.getMessage()));
        }
        
        // PHPMyAdmin 정보
        results.put("phpmyadmin", Map.of(
            "status", "info",
            "message", "📊 PHPMyAdmin은 http://localhost:9012 에서 접속 가능합니다.",
            "url", "http://localhost:9012",
            "username", "Campung",
            "database", "Campung"
        ));
        
        response.put("status", "completed");
        response.put("message", "🎉 모든 서비스 연결 테스트 완료!");
        response.put("results", results);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 테스트 데이터 생성 API
     * @param testData 생성할 테스트 데이터
     * @return 생성된 TestEntity
     * @throws Exception 생성 실패 시
     */
    @Operation(summary = "📝 테스트 데이터 생성", description = "MariaDB에 새로운 테스트 데이터를 생성합니다.")
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> createTestData(@RequestParam String testData) throws Exception {
        TestEntity created = databaseService.createTestData(testData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "테스트 데이터가 성공적으로 생성되었습니다.");
        response.put("data", created);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 테스트 데이터 조회 API
     * @return 모든 TestEntity 리스트
     * @throws Exception 조회 실패 시
     */
    @Operation(summary = "📋 테스트 데이터 전체 조회", description = "MariaDB에 저장된 모든 테스트 데이터를 조회합니다.")
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getAllTestData() throws Exception {
        List<TestEntity> data = databaseService.getAllTestData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "테스트 데이터 조회 완료");
        response.put("count", data.size());
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ID로 테스트 데이터 조회 API
     * @param id 조회할 데이터의 ID
     * @return 해당 ID의 TestEntity
     * @throws Exception 조회 실패 시
     */
    @GetMapping("/data/{id}")
    public ResponseEntity<Map<String, Object>> getTestDataById(@PathVariable Integer id) throws Exception {
        TestEntity data = databaseService.getTestDataById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "테스트 데이터 조회 완료");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 테스트 데이터 삭제 API
     * @param id 삭제할 데이터의 ID
     * @return 삭제 완료 메시지
     * @throws Exception 삭제 실패 시
     */
    @DeleteMapping("/data/{id}")
    public ResponseEntity<Map<String, Object>> deleteTestData(@PathVariable Integer id) throws Exception {
        databaseService.deleteTestData(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "ID " + id + "의 테스트 데이터가 성공적으로 삭제되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Redis에 키-값 저장 API
     * @param key 저장할 키
     * @param value 저장할 값
     * @return 저장 완료 메시지
     * @throws Exception 저장 실패 시
     */
    @PostMapping("/redis")
    public ResponseEntity<Map<String, Object>> setRedisValue(@RequestParam String key, @RequestParam String value) throws Exception {
        redisService.setValue(key, value);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Redis에 데이터가 성공적으로 저장되었습니다.");
        response.put("key", key);
        response.put("value", value);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Redis에서 값 조회 API
     * @param key 조회할 키
     * @return 해당 키의 값
     * @throws Exception 조회 실패 시
     */
    @GetMapping("/redis/{key}")
    public ResponseEntity<Map<String, Object>> getRedisValue(@PathVariable String key) throws Exception {
        String value = redisService.getValue(key);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Redis 데이터 조회 완료");
        response.put("key", key);
        response.put("value", value);
        
        return ResponseEntity.ok(response);
    }
}
