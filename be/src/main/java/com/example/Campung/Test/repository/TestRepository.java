package com.example.Campung.Test.repository;

import com.example.Campung.Test.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TestEntity에 대한 데이터 접근 인터페이스
 * 인터페이스 분리 원칙(ISP)을 준수하여 테스트 데이터 관련 기능만 정의
 */
@Repository
public interface TestRepository extends JpaRepository<TestEntity, Integer> {
    
    /**
     * test 필드 값으로 검색
     * @param test 검색할 test 값
     * @return 해당 test 값을 가진 TestEntity 리스트
     */
    List<TestEntity> findByTest(String test);
    
    /**
     * test 필드에 특정 문자열이 포함된 엔티티 검색
     * @param test 포함될 문자열
     * @return 해당 문자열이 포함된 TestEntity 리스트
     */
    List<TestEntity> findByTestContaining(String test);
}
