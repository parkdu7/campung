package com.example.campung.emotion.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 시간대별 온도 가이드라인 설정 클래스
 * 24시간 동안 자연스러운 온도 변화를 위한 설정
 */
@Component
public class TemperatureGuidelineConfig {
    
    private final Map<Integer, TemperatureGuideline> hourlyGuidelines;
    
    public TemperatureGuidelineConfig() {
        this.hourlyGuidelines = initializeGuidelines();
    }
    
    private Map<Integer, TemperatureGuideline> initializeGuidelines() {
        Map<Integer, TemperatureGuideline> guidelines = new HashMap<>();
        
        // 새벽 (0-5시): 저온 유지, 하락 위주 (0-25도)
        for (int h = 0; h <= 5; h++) {
            guidelines.put(h, new TemperatureGuideline(0.0, 25.0, 0.4, 1.8));
        }
        
        // 아침 (6-8시): 점진적 상승 (5-35도)
        for (int h = 6; h <= 8; h++) {
            guidelines.put(h, new TemperatureGuideline(5.0, 35.0, 1.5, 0.8));
        }
        
        // 오전 (9-12시): 활발한 상승 허용 (10-60도)
        for (int h = 9; h <= 12; h++) {
            guidelines.put(h, new TemperatureGuideline(10.0, 60.0, 2.0, 0.5));
        }
        
        // 오후 (13-17시): 최대 활성화 (15-100도)
        for (int h = 13; h <= 17; h++) {
            guidelines.put(h, new TemperatureGuideline(15.0, 100.0, 2.5, 0.3));
        }
        
        // 저녁 (18-20시): 높은 온도 유지 가능 (10-80도)
        for (int h = 18; h <= 20; h++) {
            guidelines.put(h, new TemperatureGuideline(10.0, 80.0, 1.8, 0.9));
        }
        
        // 밤 (21-23시): 점진적 하락 (5-50도)
        for (int h = 21; h <= 23; h++) {
            guidelines.put(h, new TemperatureGuideline(5.0, 50.0, 1.0, 1.4));
        }
        
        return guidelines;
    }
    
    /**
     * 특정 시간의 온도 가이드라인 조회
     */
    public TemperatureGuideline getGuideline(int hour) {
        return hourlyGuidelines.get(hour);
    }
    
    /**
     * 모든 가이드라인 조회 (디버깅/모니터링용)
     */
    public Map<Integer, TemperatureGuideline> getAllGuidelines() {
        return new HashMap<>(hourlyGuidelines);
    }
    
    /**
     * 시간대별 자연적 온도 회복률 조회 (확대된 범위)
     */
    public double getNaturalRecoveryRate(int hour) {
        // 활동 시간대에 더 큰 회복률 (최대 100도 고려)
        if (hour >= 9 && hour <= 12) return 5.0;   // 오전 활동 시간
        if (hour >= 14 && hour <= 17) return 8.0;  // 오후 최대 활동 시간  
        if (hour >= 19 && hour <= 21) return 4.0;  // 저녁 활동 시간
        if (hour >= 7 && hour <= 8) return 3.5;    // 아침 시간
        if (hour >= 18 && hour <= 18) return 3.0;  // 저녁 시간
        return 1.0;                                 // 기타 시간 (새벽, 늦은 밤)
    }
    
    /**
     * 온도 가이드라인 데이터 클래스
     */
    @Data
    @AllArgsConstructor
    public static class TemperatureGuideline {
        private final double minTemp;           // 최저 권장 온도
        private final double maxTemp;           // 최고 권장 온도
        private final double increaseMultiplier; // 상승률 배수
        private final double decreaseMultiplier; // 하락률 배수
        
        /**
         * 온도가 가이드라인 범위 내인지 확인
         */
        public boolean isWithinRange(double temperature) {
            return temperature >= minTemp && temperature <= maxTemp;
        }
        
        /**
         * 온도를 가이드라인 범위 내로 보정
         */
        public double adjustToRange(double temperature) {
            return Math.max(minTemp, Math.min(maxTemp, temperature));
        }
        
        /**
         * 가이드라인 정보 문자열 반환
         */
        public String getInfo() {
            return String.format("범위: %.1f-%.1f도, 상승배수: %.1f, 하락배수: %.1f", 
                    minTemp, maxTemp, increaseMultiplier, decreaseMultiplier);
        }
    }
}