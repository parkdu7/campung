package com.example.Campung.Test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * RedisService의 구현체
 * 단일 책임 원칙(SRP)을 준수하여 Redis 관련 비즈니스 로직만 처리
 * 의존성 역전 원칙(DIP)을 준수하여 추상화(인터페이스)에 의존
 */
@Service
public class RedisServiceImpl implements RedisService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 의존성 주입을 통한 생성자
     * 의존성 주입 원칙을 준수하여 생성자를 통해 의존성을 주입받음
     */
    @Autowired
    public RedisServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public String checkRedisConnection() throws Exception {
        String testKey = "connection_test";
        String testValue = "Hello Redis!";
        
        // 연결 테스트를 위한 간단한 set/get 작업
        redisTemplate.opsForValue().set(testKey, testValue);
        String retrievedValue = redisTemplate.opsForValue().get(testKey);
        
        if (testValue.equals(retrievedValue)) {
            return "✅ Redis 연결 성공! 저장된 값: " + retrievedValue;
        } else {
            throw new RuntimeException("Redis 데이터 불일치 오류");
        }
    }
    
    @Override
    public void setValue(String key, String value) throws Exception {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("키는 비어있을 수 없습니다.");
        }
        
        if (value == null) {
            throw new IllegalArgumentException("값은 null일 수 없습니다.");
        }
        
        redisTemplate.opsForValue().set(key.trim(), value);
    }
    
    @Override
    public String getValue(String key) throws Exception {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("키는 비어있을 수 없습니다.");
        }
        
        String value = redisTemplate.opsForValue().get(key.trim());
        if (value == null) {
            throw new RuntimeException("키 '" + key + "'에 해당하는 값을 찾을 수 없습니다.");
        }
        
        return value;
    }
    
    @Override
    public void deleteKey(String key) throws Exception {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("키는 비어있을 수 없습니다.");
        }
        
        Boolean deleted = redisTemplate.delete(key.trim());
        if (!Boolean.TRUE.equals(deleted)) {
            throw new RuntimeException("키 '" + key + "'를 삭제할 수 없습니다. 키가 존재하지 않을 수 있습니다.");
        }
    }
}
