package com.example.campung.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 캠퍼스 온도 추적 엔티티
 * 실시간 온도 변화 및 조정 내역을 저장
 */
@Entity
@Table(name = "campus_temperature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampusTemperature {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(nullable = false, name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(nullable = false, name = "current_temperature")
    private Double currentTemperature;
    
    @Column(nullable = false, name = "base_emotion_temperature")
    private Double baseEmotionTemperature;
    
    @Column(nullable = false, name = "post_count_adjustment")
    private Double postCountAdjustment;
    
    @Column(nullable = false, name = "current_hour_post_count")
    private Integer currentHourPostCount;
    
    @Column(nullable = false, name = "expected_hourly_average")
    private Double expectedHourlyAverage;
    
    @Column(name = "adjustment_reason")
    private String adjustmentReason;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}