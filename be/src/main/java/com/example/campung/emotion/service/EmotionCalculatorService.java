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

        log.info("감정 온도 계산 - 활력 지수: {}", vitalityIndex);

        // 활력 지수에 따른 온도 범위 계산
        if (vitalityIndex > 40) {
            // 25-35°C (뜨거움)
            return 25.0 + (random.nextDouble() * 10.0);
        } else if (vitalityIndex >= 20) {
            // 20-24°C (따뜻함)
            return 20.0 + (random.nextDouble() * 4.0);
        } else if (vitalityIndex >= -20) {
            // 15-19°C (선선함)
            return 15.0 + (random.nextDouble() * 4.0);
        } else {
            // 5-14°C (쌀쌀함)
            return 5.0 + (random.nextDouble() * 9.0);
        }
    }

    /**
     * 온도 값을 소수점 첫째 자리로 반올림
     */
    public Double roundTemperature(Double temperature) {
        return Math.round(temperature * 10.0) / 10.0;
    }
}