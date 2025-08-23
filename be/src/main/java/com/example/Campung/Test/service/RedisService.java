package com.example.Campung.Test.service;

/**
 * Redis 관련 비즈니스 로직을 정의하는 서비스 인터페이스
 * 의존성 역전 원칙(DIP)을 준수하여 추상화에 의존하도록 함
 * 인터페이스 분리 원칙(ISP)을 준수하여 Redis 관련 기능만 정의
 */
public interface RedisService {
    
    /**
     * Redis 연결 상태를 확인
     * @return 연결 상태 메시지
     * @throws Exception Redis 연결 실패 시
     */
    String checkRedisConnection() throws Exception;
    
    /**
     * Redis에 키-값 쌍을 저장
     * @param key 저장할 키
     * @param value 저장할 값
     * @throws Exception 저장 실패 시
     */
    void setValue(String key, String value) throws Exception;
    
    /**
     * Redis에서 값을 조회
     * @param key 조회할 키
     * @return 해당 키의 값
     * @throws Exception 조회 실패 시
     */
    String getValue(String key) throws Exception;
    
    /**
     * Redis에서 키를 삭제
     * @param key 삭제할 키
     * @throws Exception 삭제 실패 시
     */
    void deleteKey(String key) throws Exception;
}
