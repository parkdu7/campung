package com.example.campung.emotion.service;

import com.example.campung.global.enums.EmotionType;
import com.example.campung.global.enums.WeatherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

/**
 * 감정 점수를 기반으로 날씨와 온도를 계산하는 서비스
 */
@Service
@Slf4j
public class EmotionCalculatorService {

    private final Random random = new Random();

    /**
     * 감정 점수를 기반으로 감정 날씨 결정
     */
    public WeatherType calculateEmotionWeather(Map<String, Double> averageScores) {
        double positiveScore = averageScores.get(EmotionType.BRIGHTNESS.getKoreanName()) + 
                              averageScores.get(EmotionType.EXCITEMENT.getKoreanName()) + 
                              averageScores.get(EmotionType.THRILLED.getKoreanName());
        
        double negativeScore = averageScores.get(EmotionType.DEPRESSION.getKoreanName()) + 
                              averageScores.get(EmotionType.ANGER.getKoreanName()) + 
                              averageScores.get(EmotionType.SADNESS.getKoreanName());

        log.info("감정 날씨 계산 - 긍정: {}, 부정: {}", positiveScore, negativeScore);

        // 긍정적 감정이 우세한 경우
        if (positiveScore >= negativeScore) {
            if (positiveScore >= 240) { // 80점 이상 (3개 감정 평균)
                return WeatherType.SUNNY;
            } else if (positiveScore >= 180) { // 60-79점
                return WeatherType.PARTLY_CLOUDY;
            } else {
                return WeatherType.CLOUDY;
            }
        } 
        // 부정적 감정이 우세한 경우
        else {
            if (negativeScore >= 180) { // 60점 이상 (3개 감정 평균)
                return WeatherType.RAINY;
            } else if (negativeScore >= 120) { // 40-59점
                return WeatherType.MOSTLY_CLOUDY;
            } else {
                return WeatherType.CLOUDY;
            }
        }
    }

    /**
     * 활력 지수를 기반으로 감정 온도 계산
     */
    public Double calculateEmotionTemperature(Map<String, Double> averageScores) {
        double vitalityIndex = (averageScores.get(EmotionType.EXCITEMENT.getKoreanName()) + 
                               averageScores.get(EmotionType.THRILLED.getKoreanName())) - 
                              (averageScores.get(EmotionType.DEPRESSION.getKoreanName()) + 
                               averageScores.get(EmotionType.SADNESS.getKoreanName()));

        // 전체 감정 점수 평균 계산
        double totalEmotionAverage = averageScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        log.info("감정 온도 계산 - 활력 지수: {}, 전체 감정 평균: {}", vitalityIndex, totalEmotionAverage);

        // 감정 점수가 전반적으로 낮은 경우 온도를 더 낮게 조정
        double temperatureAdjustment = 0.0;
        if (totalEmotionAverage < 30) {
            temperatureAdjustment = -8.0; // 매우 낮은 감정 점수
            log.info("감정 점수가 매우 낮음 (평균 {}점) - 온도 {}도 하락", totalEmotionAverage, temperatureAdjustment);
        } else if (totalEmotionAverage < 50) {
            temperatureAdjustment = -5.0; // 낮은 감정 점수
            log.info("감정 점수가 낮음 (평균 {}점) - 온도 {}도 하락", totalEmotionAverage, temperatureAdjustment);
        }

        // 활력 지수에 따른 온도 범위 계산
        double baseTemperature;
        if (vitalityIndex > 40) {
            // 25-35°C (뜨거움)
            baseTemperature = 25.0 + (random.nextDouble() * 10.0);
        } else if (vitalityIndex >= 20) {
            // 20-24°C (따뜻함)
            baseTemperature = 20.0 + (random.nextDouble() * 4.0);
        } else if (vitalityIndex >= -20) {
            // 15-19°C (선선함)
            baseTemperature = 15.0 + (random.nextDouble() * 4.0);
        } else {
            // 5-14°C (쌀쌀함)
            baseTemperature = 5.0 + (random.nextDouble() * 9.0);
        }

        // 최종 온도에 감정 점수 조정값 적용 (최저 1도로 제한)
        double finalTemperature = Math.max(1.0, baseTemperature + temperatureAdjustment);
        log.info("최종 온도 계산: 기본 {}도 + 조정 {}도 = {}도", baseTemperature, temperatureAdjustment, finalTemperature);
        
        return finalTemperature;
    }

    /**
     * 온도 값을 소수점 첫째 자리로 반올림
     */
    public Double roundTemperature(Double temperature) {
        return Math.round(temperature * 10.0) / 10.0;
    }
}