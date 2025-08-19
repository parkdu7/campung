package com.example.Campung.Test.entity;

import jakarta.persistence.*;

/**
 * 테스트를 위한 간단한 엔티티 클래스
 * 단일 책임 원칙(SRP)을 준수하여 데이터베이스 테이블과의 매핑만을 담당
 */
@Entity
@Table(name = "test")
public class TestEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "test", nullable = false)
    private String test;
    
    // 기본 생성자 (JPA 필수)
    public TestEntity() {}
    
    // 생성자
    public TestEntity(String test) {
        this.test = test;
    }
    
    // Getter와 Setter
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getTest() {
        return test;
    }
    
    public void setTest(String test) {
        this.test = test;
    }
    
    @Override
    public String toString() {
        return "TestEntity{" +
                "id=" + id +
                ", test='" + test + '\'' +
                '}';
    }
}
