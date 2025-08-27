package com.example.campung.emotion.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.emotion.repository.DailyCampusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 게시글 활동 분석 클래스
 * 게시글 수를 기반으로 캠퍼스 활동도를 분석
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostActivityAnalyzer {
    
    private final ContentRepository contentRepository;
    private final DailyCampusRepository dailyCampusRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CACHE_KEY_PREFIX = "post:activity:";
    
    /**
     * 현재 시간 게시글 수 조회
     */
    public int getCurrentHourPostCount() {
        LocalDateTime hourStart = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime hourEnd = hourStart.plusHours(1);
        
        String cacheKey = CACHE_KEY_PREFIX + "current_hour:" + hourStart.toString();
        Integer cached = (Integer) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        int count = contentRepository.countByCreatedAtBetween(hourStart, hourEnd);
        
        // 10분 캐시
        redisTemplate.opsForValue().set(cacheKey, count, Duration.ofMinutes(10));
        
        log.info("현재 시간({}) 게시글 수: {}개", hourStart.getHour(), count);
        return count;
    }
    
    /**
     * 최근 N일 기준 일평균 게시글 수 계산
     */
    public double calculateDailyAveragePostCount(int days) {
        String cacheKey = CACHE_KEY_PREFIX + "daily_average:" + days;
        Double cached = (Double) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        LocalDate startDate = LocalDate.now().minusDays(days);
        Double average = dailyCampusRepository.findAveragePostCountSince(startDate);
        
        if (average == null) {
            // DailyCampus 데이터가 없는 경우 Content 테이블에서 직접 계산
            average = calculateDailyAverageFromContent(days);
        }
        
        // 1시간 캐시
        redisTemplate.opsForValue().set(cacheKey, average, Duration.ofHours(1));
        
        log.info("최근 {}일 일평균 게시글 수: {}개", days, average);
        return average;
    }
    
    /**
     * Content 테이블에서 직접 일평균 계산 (fallback)
     */
    private double calculateDailyAverageFromContent(int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        int totalCount = contentRepository.countByCreatedAtBetween(startTime, endTime);
        return (double) totalCount / days;
    }
    
    /**
     * 시간당 예상 평균 게시글 수
     */
    public double getExpectedHourlyAverage() {
        Double dailyAverage = dailyCampusRepository.findAverageHourlyPostCountSince(LocalDate.now().minusDays(7));
        
        if (dailyAverage != null) {
            return dailyAverage;
        }
        
        // fallback: 일평균을 24로 나눔
        return calculateDailyAveragePostCount(7) / 24.0;
    }
    
    /**
     * 게시글 활동 패턴 분석 (확장된 레벨 지원)
     */
    public PostActivityLevel analyzeCurrentActivity(int currentPosts) {
        double expected = getExpectedHourlyAverage();
        
        if (expected == 0) {
            return PostActivityLevel.NORMAL;
        }
        
        double ratio = currentPosts / expected;
        
        if (ratio >= 3.0) return PostActivityLevel.EXTREMELY_HIGH;  // 300% 이상
        if (ratio >= 2.0) return PostActivityLevel.VERY_HIGH;       // 200-299%
        if (ratio >= 1.5) return PostActivityLevel.HIGH;            // 150-199%
        if (ratio >= 1.2) return PostActivityLevel.MODERATE_HIGH;   // 120-149%
        if (ratio >= 0.8) return PostActivityLevel.NORMAL;          // 80-119%
        if (ratio >= 0.6) return PostActivityLevel.MODERATE_LOW;    // 60-79%
        if (ratio >= 0.4) return PostActivityLevel.LOW;             // 40-59%
        if (ratio >= 0.2) return PostActivityLevel.VERY_LOW;        // 20-39%
        return PostActivityLevel.EXTREMELY_LOW;                     // 20% 미만
    }
    
    /**
     * 특정 시간대의 예상 게시글 수 (과거 패턴 기반)
     */
    public int getExpectedPostsForHour(int hour) {
        // 시간대별 활동 패턴 (경험적 데이터)
        double[] hourlyMultipliers = {
            0.2, 0.1, 0.05, 0.05, 0.1, 0.3,  // 0-5시: 새벽
            0.8, 1.2, 1.5, 1.8, 2.0, 1.9,   // 6-11시: 오전
            1.7, 1.8, 1.9, 2.0, 1.8, 1.5,   // 12-17시: 오후
            1.3, 1.4, 1.2, 1.0, 0.8, 0.5    // 18-23시: 저녁/밤
        };
        
        double expectedHourly = getExpectedHourlyAverage();
        return (int) Math.round(expectedHourly * hourlyMultipliers[hour]);
    }
    
    /**
     * 게시글 활동 레벨 열거형 (확장된 온도 범위 0-100도 지원)
     */
    public enum PostActivityLevel {
        EXTREMELY_HIGH(0.35, -0.08, "극도로 활발"),    // 300% 이상
        VERY_HIGH(0.25, -0.10, "매우 활발"),          // 200-299%
        HIGH(0.15, -0.08, "활발"),                   // 150-199%
        MODERATE_HIGH(0.08, -0.05, "약간 활발"),      // 120-149%
        NORMAL(0.0, 0.0, "보통"),                   // 80-119%
        MODERATE_LOW(-0.03, 0.05, "약간 저조"),      // 60-79%
        LOW(-0.08, 0.08, "저조"),                   // 40-59%
        VERY_LOW(-0.12, 0.12, "매우 저조"),         // 20-39%
        EXTREMELY_LOW(-0.15, 0.18, "극도로 저조");   // 20% 미만
        
        private final double increaseRate;    // 활발할 때 온도 상승률
        private final double decreaseRate;    // 저조할 때 온도 하락률  
        private final String description;
        
        PostActivityLevel(double increaseRate, double decreaseRate, String description) {
            this.increaseRate = increaseRate;
            this.decreaseRate = decreaseRate;
            this.description = description;
        }
        
        public double getAdjustmentRate() {
            return increaseRate != 0 ? increaseRate : decreaseRate;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isIncreasing() {
            return increaseRate > 0;
        }
    }
}