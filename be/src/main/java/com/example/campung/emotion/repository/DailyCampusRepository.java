package com.example.campung.emotion.repository;

import com.example.campung.entity.DailyCampus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCampusRepository extends JpaRepository<DailyCampus, Long> {
    
    /**
     * 특정 날짜의 캠퍼스 데이터 조회
     */
    Optional<DailyCampus> findByDate(LocalDate date);
    
    /**
     * 최근 N일간의 캠퍼스 데이터 조회
     */
    @Query("SELECT dc FROM DailyCampus dc WHERE dc.date >= :startDate ORDER BY dc.date DESC")
    List<DailyCampus> findRecentDays(@Param("startDate") LocalDate startDate);
    
    /**
     * 최근 N일간의 평균 게시글 수 계산
     */
    @Query("SELECT AVG(dc.totalPostCount) FROM DailyCampus dc WHERE dc.date >= :startDate")
    Double findAveragePostCountSince(@Param("startDate") LocalDate startDate);
    
    /**
     * 최근 N일간의 평균 시간당 게시글 수 계산
     */
    @Query("SELECT AVG(dc.averageHourlyPostCount) FROM DailyCampus dc WHERE dc.date >= :startDate")
    Double findAverageHourlyPostCountSince(@Param("startDate") LocalDate startDate);
    
    /**
     * 가장 최근 데이터 조회
     */
    Optional<DailyCampus> findFirstByOrderByDateDesc();
}