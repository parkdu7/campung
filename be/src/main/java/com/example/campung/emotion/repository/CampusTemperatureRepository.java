package com.example.campung.emotion.repository;

import com.example.campung.entity.CampusTemperature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampusTemperatureRepository extends JpaRepository<CampusTemperature, Long> {
    
    /**
     * 가장 최근 온도 기록 조회
     */
    Optional<CampusTemperature> findFirstByOrderByTimestampDesc();
    
    /**
     * 특정 시간 범위 내 온도 기록 조회
     */
    @Query("SELECT ct FROM CampusTemperature ct WHERE ct.timestamp BETWEEN :startTime AND :endTime ORDER BY ct.timestamp DESC")
    List<CampusTemperature> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 특정 날짜의 최고/최저 온도 조회
     */
    @Query("SELECT MAX(ct.currentTemperature), MIN(ct.currentTemperature) FROM CampusTemperature ct WHERE DATE(ct.timestamp) = DATE(:date)")
    Object[] findMaxMinTemperatureByDate(@Param("date") LocalDateTime date);
    
    /**
     * 최근 N시간의 온도 기록 조회
     */
    @Query("SELECT ct FROM CampusTemperature ct WHERE ct.timestamp >= :since ORDER BY ct.timestamp DESC")
    List<CampusTemperature> findRecentHours(@Param("since") LocalDateTime since);
    
    /**
     * 오래된 기록 삭제 (성능 최적화용)
     */
    void deleteByTimestampBefore(LocalDateTime cutoffTime);
}