package com.example.Campung.Test.service;

import com.example.Campung.Test.entity.TestEntity;
import com.example.Campung.Test.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * DatabaseService의 구현체
 * 단일 책임 원칙(SRP)을 준수하여 데이터베이스 관련 비즈니스 로직만 처리
 * 의존성 역전 원칙(DIP)을 준수하여 추상화(인터페이스)에 의존
 */
@Service
public class DatabaseServiceImpl implements DatabaseService {
    
    private final TestRepository testRepository;
    private final DataSource dataSource;
    
    /**
     * 의존성 주입을 통한 생성자
     * 의존성 주입 원칙을 준수하여 생성자를 통해 의존성을 주입받음
     */
    @Autowired
    public DatabaseServiceImpl(TestRepository testRepository, DataSource dataSource) {
        this.testRepository = testRepository;
        this.dataSource = dataSource;
    }
    
    @Override
    public String checkDatabaseConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            return "✅ MariaDB 연결 성공! Database: " + conn.getCatalog();
        }
    }
    
    @Override
    public TestEntity createTestData(String testData) throws Exception {
        if (testData == null || testData.trim().isEmpty()) {
            throw new IllegalArgumentException("테스트 데이터는 비어있을 수 없습니다.");
        }
        
        TestEntity entity = new TestEntity(testData.trim());
        return testRepository.save(entity);
    }
    
    @Override
    public List<TestEntity> getAllTestData() throws Exception {
        return testRepository.findAll();
    }
    
    @Override
    public TestEntity getTestDataById(Integer id) throws Exception {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID입니다.");
        }
        
        return testRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ID " + id + "에 해당하는 데이터를 찾을 수 없습니다."));
    }
    
    @Override
    public void deleteTestData(Integer id) throws Exception {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID입니다.");
        }
        
        if (!testRepository.existsById(id)) {
            throw new RuntimeException("ID " + id + "에 해당하는 데이터를 찾을 수 없습니다.");
        }
        
        testRepository.deleteById(id);
    }
}
