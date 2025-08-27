package com.example.campung.entity;

import com.example.campung.global.enums.WeatherType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일일 캠퍼스 통계 엔티티
 * 매일 캠퍼스 온도, 날씨, 게시글 수 등을 저장
 */
@Entity
@Table(name = "daily_campus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCampus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(nullable = false, unique = true, name = "date")
    private LocalDate date;
    
    @Column(nullable = false, name = "final_temperature")
    private Double finalTemperature;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "weather_type")
    private WeatherType weatherType;
    
    @Column(nullable = false, name = "total_post_count")
    private Integer totalPostCount;
    
    @Column(nullable = false, name = "average_hourly_post_count")
    private Double averageHourlyPostCount;
    
    @Column(nullable = false, name = "max_temperature")
    private Double maxTemperature;
    
    @Column(nullable = false, name = "min_temperature")
    private Double minTemperature;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}