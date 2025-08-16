package com.example.Campung.Test.service;

import com.example.Campung.Test.entity.TestEntity;

import java.util.List;

/**
 * 데이터베이스 관련 비즈니스 로직을 정의하는 서비스 인터페이스
 * 의존성 역전 원칙(DIP)을 준수하여 추상화에 의존하도록 함
 * 인터페이스 분리 원칙(ISP)을 준수하여 데이터베이스 관련 기능만 정의
 */
public interface DatabaseService {
    
    /**
     * 데이터베이스 연결 상태를 확인
     * @return 연결 상태 메시지
     * @throws Exception 데이터베이스 연결 실패 시
     */
    String checkDatabaseConnection() throws Exception;
    
    /**
     * 새로운 테스트 데이터를 생성
     * @param testData 저장할 테스트 데이터
     * @return 저장된 TestEntity
     * @throws Exception 저장 실패 시
     */
    TestEntity createTestData(String testData) throws Exception;
    
    /**
     * 모든 테스트 데이터를 조회
     * @return 모든 TestEntity 리스트
     * @throws Exception 조회 실패 시
     */
    List<TestEntity> getAllTestData() throws Exception;
    
    /**
     * ID로 테스트 데이터를 조회
     * @param id 조회할 데이터의 ID
     * @return 해당 ID의 TestEntity
     * @throws Exception 조회 실패 시
     */
    TestEntity getTestDataById(Integer id) throws Exception;
    
    /**
     * 테스트 데이터를 삭제
     * @param id 삭제할 데이터의 ID
     * @throws Exception 삭제 실패 시
     */
    void deleteTestData(Integer id) throws Exception;
}
