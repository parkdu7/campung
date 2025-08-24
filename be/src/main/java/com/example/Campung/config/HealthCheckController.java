package com.example.Campung.Config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Campung API");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Campung API");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("redis", "UP");
        response.put("components", components);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readiness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "READY");
        response.put("message", "Application is ready to serve traffic");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ALIVE");
        response.put("message", "Application is running");
        return ResponseEntity.ok(response);
    }
}