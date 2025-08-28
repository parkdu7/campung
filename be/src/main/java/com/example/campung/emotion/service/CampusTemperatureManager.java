package com.example.campung.emotion.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.entity.CampusTemperature;
import com.example.campung.entity.DailyCampus;
import com.example.campung.emotion.config.TemperatureGuidelineConfig;
import com.example.campung.emotion.repository.CampusTemperatureRepository;
import com.example.campung.emotion.repository.DailyCampusRepository;
import com.example.campung.global.enums.WeatherType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 캠퍼스 온도 동적 관리 시스템
 * 감정 분석 + 게시글 활동도를 기반으로 온도를 동적 조정
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampusTemperatureManager {
    
    private final CampusTemperatureRepository temperatureRepository;
    private final DailyCampusRepository dailyCampusRepository;
    private final ContentRepository contentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostActivityAnalyzer postActivityAnalyzer;
    private final TemperatureGuidelineConfig guidelineConfig;
    
    // Redis 키 상수
    private static final String CURRENT_TEMP_KEY = "campus:temperature:current";
    private static final String BASE_EMOTION_TEMP_KEY = "campus:temperature:base_emotion";
    private static final String TEMPERATURE_PROTECTION_KEY = "campus:temperature:protection_mode";
    private static final String TODAY_MAX_TEMP_KEY = "campus:temperature:today_max";
    private static final String TODAY_MIN_TEMP_KEY = "campus:temperature:today_min";
    
    /**
     * 메인 온도 조정 메서드 - 매시간 호출
     */
    @Transactional
    public void adjustHourlyTemperature() {
        int currentHour = LocalTime.now().getHour();
        int hourlyPostCount = postActivityAnalyzer.getCurrentHourPostCount();
        double currentTemp = getCurrentCampusTemperature();
        
        log.info("=== 시간당 온도 조정 시작 ===");
        log.info("현재 시간: {}시, 게시글 수: {}개, 현재 온도: {}도", currentHour, hourlyPostCount, currentTemp);
        
        // 양방향 온도 조정 계산
        double adjustedTemp = calculateBidirectionalAdjustment(currentTemp, hourlyPostCount, currentHour);
        
        // 가이드라인 범위 내 보정
        double finalTemp = applyTemperatureGuidelines(adjustedTemp, currentHour);
        
        // 온도 업데이트 및 기록 저장
        updateCampusTemperature(finalTemp, hourlyPostCount, "hourly_adjustment");
        
        log.info("=== 시간당 온도 조정 완료: {}도 -> {}도 ===", currentTemp, finalTemp);
    }
    
    /**
     * 양방향 온도 조정 계산
     */
    private double calculateBidirectionalAdjustment(double currentTemp, int postCount, int hour) {
        PostActivityAnalyzer.PostActivityLevel activityLevel = postActivityAnalyzer.analyzeCurrentActivity(postCount);
        
        // 기본 조정률
        double baseAdjustmentRate = activityLevel.getAdjustmentRate();
        
        // 온도 보호 계수 적용
        double protectionFactor = getTemperatureProtectionFactor(currentTemp, activityLevel.isIncreasing());
        
        // 시간대별 활동 패턴 반영
        TemperatureGuidelineConfig.TemperatureGuideline guideline = guidelineConfig.getGuideline(hour);
        double timeMultiplier = activityLevel.isIncreasing() ? 
                guideline.getIncreaseMultiplier() : guideline.getDecreaseMultiplier();
        
        // 최종 조정률
        double finalRate = baseAdjustmentRate * protectionFactor * timeMultiplier;
        
        double adjustedTemp = currentTemp * (1 + finalRate);
        
        log.info("온도 조정 계산 - 활동도: {}, 기본조정률: {}%, 보호계수: {}, 시간배수: {}, 최종조정률: {}%",
                activityLevel.getDescription(), baseAdjustmentRate * 100, protectionFactor, 
                timeMultiplier, finalRate * 100);
        
        return adjustedTemp;
    }
    
    /**
     * 온도 보호 계수 계산 (0-100도 범위에서 극한 온도 방지)
     */
    private double getTemperatureProtectionFactor(double currentTemp, boolean isIncreasing) {
        if (isIncreasing) {
            // 상승 시 극고온 보호 (100도 상한)
            if (currentTemp >= 95.0) return 0.05;     // 95% 억제
            if (currentTemp >= 90.0) return 0.2;      // 80% 억제
            if (currentTemp >= 80.0) return 0.4;      // 60% 억제
            if (currentTemp >= 70.0) return 0.6;      // 40% 억제
            if (currentTemp >= 60.0) return 0.8;      // 20% 억제
            return 1.0;                                // 정상 (60도 미만)
        } else {
            // 하락 시 저온 보호 (0도 하한)
            if (currentTemp <= 2.0) return 0.05;      // 95% 억제
            if (currentTemp <= 5.0) return 0.2;       // 80% 억제
            if (currentTemp <= 10.0) return 0.4;      // 60% 억제
            if (currentTemp <= 15.0) return 0.6;      // 40% 억제
            if (currentTemp <= 20.0) return 0.8;      // 20% 억제
            return 1.0;                                // 정상 (20도 초과)
        }
    }
    
    /**
     * 온도 가이드라인 범위 내 보정
     */
    private double applyTemperatureGuidelines(double temperature, int hour) {
        TemperatureGuidelineConfig.TemperatureGuideline guideline = guidelineConfig.getGuideline(hour);
        double adjustedTemp = guideline.adjustToRange(temperature);
        
        if (adjustedTemp != temperature) {
            log.info("가이드라인 보정 적용: {}도 -> {}도 ({}시 기준: {})", 
                    temperature, adjustedTemp, hour, guideline.getInfo());
        }
        
        return adjustedTemp;
    }
    
    /**
     * 현재 캠퍼스 온도 조회
     */
    public double getCurrentCampusTemperature() {
        Double cached = (Double) redisTemplate.opsForValue().get(CURRENT_TEMP_KEY);
        if (cached != null) {
            return cached;
        }
        
        // Redis에 없으면 DB에서 조회
        return temperatureRepository.findFirstByOrderByTimestampDesc()
                .map(CampusTemperature::getCurrentTemperature)
                .orElse(20.0); // 기본값
    }
    
    /**
     * 캠퍼스 온도 업데이트
     */
    @Transactional
    public void updateCampusTemperature(double newTemp, int postCount, String reason) {
        double baseEmotionTemp = getBaseEmotionTemperature();
        double postAdjustment = newTemp - baseEmotionTemp;
        double expectedAverage = postActivityAnalyzer.getExpectedHourlyAverage();
        
        // DB에 기록 저장
        CampusTemperature record = CampusTemperature.builder()
                .timestamp(LocalDateTime.now())
                .currentTemperature(Math.round(newTemp * 10.0) / 10.0)
                .baseEmotionTemperature(baseEmotionTemp)
                .postCountAdjustment(Math.round(postAdjustment * 10.0) / 10.0)
                .currentHourPostCount(postCount)
                .expectedHourlyAverage(Math.round(expectedAverage * 10.0) / 10.0)
                .adjustmentReason(reason)
                .build();
        
        temperatureRepository.save(record);
        
        // Redis에 현재 온도 캐시
        redisTemplate.opsForValue().set(CURRENT_TEMP_KEY, newTemp, Duration.ofMinutes(30));
        
        // 실시간 최고/최저 온도 업데이트
        updateTodayMinMaxTemperature(newTemp);
        
        log.info("캠퍼스 온도 업데이트: {}도 (감정기준: {}도, 게시글조정: {:+.1f}도, 사유: {})", 
                newTemp, baseEmotionTemp, postAdjustment, reason);
    }
    
    /**
     * 감정 분석 기반 기본 온도 설정
     */
    public void setBaseEmotionTemperature(double emotionTemp) {
        redisTemplate.opsForValue().set(BASE_EMOTION_TEMP_KEY, emotionTemp, Duration.ofHours(2));
        log.info("감정 기본 온도 설정: {}도", emotionTemp);
    }
    
    /**
     * 감정 분석 결과를 받아서 온도를 업데이트하고 기록
     */
    public void updateTemperatureFromEmotionAnalysis(double emotionTemp) {
        // 감정 기본 온도 설정
        setBaseEmotionTemperature(emotionTemp);
        
        // 현재 온도를 감정 온도로 업데이트하고 DB에 기록
        updateCampusTemperature(emotionTemp, 0, "emotion_analysis_update");
        
        log.info("감정 분석 결과로 온도 업데이트: {}도", emotionTemp);
    }
    
    /**
     * 감정 기본 온도 조회
     */
    private double getBaseEmotionTemperature() {
        Double cached = (Double) redisTemplate.opsForValue().get(BASE_EMOTION_TEMP_KEY);
        return cached != null ? cached : 20.0;
    }
    
    /**
     * 오늘의 최고/최저 온도 실시간 업데이트
     */
    private void updateTodayMinMaxTemperature(double newTemp) {
        // 현재 최고 온도 조회 및 업데이트
        Double currentMax = (Double) redisTemplate.opsForValue().get(TODAY_MAX_TEMP_KEY);
        if (currentMax == null || newTemp > currentMax) {
            redisTemplate.opsForValue().set(TODAY_MAX_TEMP_KEY, newTemp, Duration.ofHours(24));
            log.debug("오늘 최고 온도 갱신: {}도", newTemp);
        }
        
        // 현재 최저 온도 조회 및 업데이트
        Double currentMin = (Double) redisTemplate.opsForValue().get(TODAY_MIN_TEMP_KEY);
        if (currentMin == null || newTemp < currentMin) {
            redisTemplate.opsForValue().set(TODAY_MIN_TEMP_KEY, newTemp, Duration.ofHours(24));
            log.debug("오늘 최저 온도 갱신: {}도", newTemp);
        }
    }
    
    /**
     * 오늘의 최고/최저 온도 조회
     */
    public double[] getTodayMinMaxTemperature() {
        Double maxTemp = (Double) redisTemplate.opsForValue().get(TODAY_MAX_TEMP_KEY);
        Double minTemp = (Double) redisTemplate.opsForValue().get(TODAY_MIN_TEMP_KEY);
        
        // 데이터가 없으면 현재 온도로 초기화
        if (maxTemp == null || minTemp == null) {
            double currentTemp = getCurrentCampusTemperature();
            maxTemp = maxTemp != null ? maxTemp : currentTemp;
            minTemp = minTemp != null ? minTemp : currentTemp;
            
            redisTemplate.opsForValue().set(TODAY_MAX_TEMP_KEY, maxTemp, Duration.ofHours(24));
            redisTemplate.opsForValue().set(TODAY_MIN_TEMP_KEY, minTemp, Duration.ofHours(24));
        }
        
        return new double[]{maxTemp, minTemp};
    }
    
    /**
     * 자연적 온도 회복 (매시 30분 실행)
     */
    @Transactional
    public void naturalTemperatureRecovery() {
        double currentTemp = getCurrentCampusTemperature();
        int currentHour = LocalTime.now().getHour();
        
        // 활동 시간대에만 자연 회복 적용
        if (currentHour >= 7 && currentHour <= 22) {
            double recoveryRate = guidelineConfig.getNaturalRecoveryRate(currentHour);
            double recoveredTemp = currentTemp + (Math.random() * recoveryRate);
            
            TemperatureGuidelineConfig.TemperatureGuideline guideline = guidelineConfig.getGuideline(currentHour);
            double maxRecovery = guideline.adjustToRange(recoveredTemp);
            
            if (maxRecovery > currentTemp) {
                updateCampusTemperature(maxRecovery, 0, "natural_recovery");
                log.info("자연 온도 회복: {}도 -> {}도", currentTemp, maxRecovery);
            }
        }
    }
    
    /**
     * 다음날 아침 온도 예측 및 사전 조정 (확장된 범위)
     */
    @Transactional
    public void predictAndAdjustForMorning() {
        double predictedTemp = predictMorningTemperature();
        
        if (predictedTemp < 5.0) {
            log.warn("아침 예상 온도 미달 ({}도) - 사전 보정 적용", predictedTemp);
            
            // 온도 보호 모드 활성화
            redisTemplate.opsForValue().set(TEMPERATURE_PROTECTION_KEY, true, Duration.ofHours(8));
            
            // 현재 온도를 상승 조정 (더 적극적)
            double currentTemp = getCurrentCampusTemperature();
            double adjustedTemp = currentTemp + (15.0 - predictedTemp) * 0.5;
            updateCampusTemperature(adjustedTemp, 0, "morning_prediction_adjustment");
            
            log.info("사전 온도 보정: {}도 -> {}도", currentTemp, adjustedTemp);
        }
    }
    
    /**
     * 다음날 아침 온도 예측
     */
    private double predictMorningTemperature() {
        double currentTemp = getCurrentCampusTemperature();
        int currentHour = LocalTime.now().getHour();
        
        double predictedTemp = currentTemp;
        
        // 현재 시간부터 다음날 6시까지의 예상 변화 계산
        for (int h = currentHour + 1; h <= 24 + 6; h++) {
            int hour = h % 24;
            
            int expectedPosts = postActivityAnalyzer.getExpectedPostsForHour(hour);
            double expectedAverage = postActivityAnalyzer.getExpectedHourlyAverage();
            
            if (expectedPosts <= expectedAverage * 0.5) {
                TemperatureGuidelineConfig.TemperatureGuideline guideline = guidelineConfig.getGuideline(hour);
                PostActivityAnalyzer.PostActivityLevel level = postActivityAnalyzer.analyzeCurrentActivity(expectedPosts);
                double reductionRate = level.getAdjustmentRate() * guideline.getDecreaseMultiplier();
                reductionRate *= getTemperatureProtectionFactor(predictedTemp, false);
                
                predictedTemp *= (1 + reductionRate);
            }
            
            // 자연 회복 반영
            if (hour >= 7 && hour <= 22) {
                predictedTemp += guidelineConfig.getNaturalRecoveryRate(hour) * 0.3;
            }
        }
        
        return predictedTemp;
    }
    
    /**
     * 아침 최저 온도 보장 (확장된 범위)
     */
    @Transactional
    public void ensureMinimumMorningTemperature() {
        double currentTemp = getCurrentCampusTemperature();
        
        if (currentTemp < 5.0) {
            double adjustedTemp = 5.0 + (Math.random() * 15); // 5-20도 랜덤
            updateCampusTemperature(adjustedTemp, 0, "morning_minimum_guarantee");
            log.warn("아침 최저 온도 보장: {}도 -> {}도", currentTemp, adjustedTemp);
        } else {
            log.info("아침 온도 점검 완료: {}도", currentTemp);
        }
        
        // 보호 모드 해제
        redisTemplate.delete(TEMPERATURE_PROTECTION_KEY);
    }
    
    /**
     * 일일 캠퍼스 데이터 저장
     */
    @Transactional
    public void saveDailyCampusData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // 어제 온도 통계 계산
        LocalDateTime startTime = yesterday.atStartOfDay();
        LocalDateTime endTime = yesterday.plusDays(1).atStartOfDay();
        
        Object[] minMaxTemp = temperatureRepository.findMaxMinTemperatureByDate(startTime);
        double maxTemp = minMaxTemp[0] != null ? (Double) minMaxTemp[0] : 20.0;
        double minTemp = minMaxTemp[1] != null ? (Double) minMaxTemp[1] : 20.0;
        
        // 어제 게시글 통계
        int totalPosts = contentRepository.countByCreatedAtBetween(startTime, endTime);
        double avgHourlyPosts = totalPosts / 24.0;
        
        // 최종 온도 (어제 마지막 온도)
        double finalTemp = temperatureRepository.findByTimestampBetween(startTime, endTime)
                .stream()
                .findFirst()
                .map(CampusTemperature::getCurrentTemperature)
                .orElse(20.0);
        
        // DailyCampus 저장
        DailyCampus dailyCampus = DailyCampus.builder()
                .date(yesterday)
                .finalTemperature(Math.round(finalTemp * 10.0) / 10.0)
                .weatherType(WeatherType.CLOUDY) // 기본값 (추후 날씨 연동 가능)
                .totalPostCount(totalPosts)
                .averageHourlyPostCount(Math.round(avgHourlyPosts * 10.0) / 10.0)
                .maxTemperature(Math.round(maxTemp * 10.0) / 10.0)
                .minTemperature(Math.round(minTemp * 10.0) / 10.0)
                .build();
        
        dailyCampusRepository.save(dailyCampus);
        
        log.info("일일 캠퍼스 데이터 저장 완료 - 날짜: {}, 게시글: {}개, 온도: {}-{}도", 
                yesterday, totalPosts, minTemp, maxTemp);
        
        // 오늘의 최고/최저 온도 Redis 초기화 (새로운 하루 시작)
        resetTodayMinMaxTemperature();
        
        // 온도 기록 영구 보존: cleanupOldTemperatureRecords() 호출 제거
    }
    
    /**
     * 오늘의 최고/최저 온도 Redis 초기화
     */
    private void resetTodayMinMaxTemperature() {
        redisTemplate.delete(TODAY_MAX_TEMP_KEY);
        redisTemplate.delete(TODAY_MIN_TEMP_KEY);
        
        // 현재 온도로 초기화
        double currentTemp = getCurrentCampusTemperature();
        redisTemplate.opsForValue().set(TODAY_MAX_TEMP_KEY, currentTemp, Duration.ofHours(24));
        redisTemplate.opsForValue().set(TODAY_MIN_TEMP_KEY, currentTemp, Duration.ofHours(24));
        
        log.info("오늘의 최고/최저 온도 초기화 완료: {}도", currentTemp);
    }
    
    /**
     * 특정 날짜의 DailyCampus 데이터 조회
     */
    public DailyCampus getDailyCampusData(LocalDate date) {
        return dailyCampusRepository.findByDate(date).orElse(null);
    }
    
}