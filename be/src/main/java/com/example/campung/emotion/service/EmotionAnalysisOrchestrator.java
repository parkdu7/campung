package com.example.campung.emotion.service;

import com.example.campung.emotion.service.EmotionAnalysisService.PostData;
import com.example.campung.emotion.service.TimeRangeAnalysisService.AnalysisTimeRange;
import com.example.campung.entity.Content;
import com.example.campung.global.enums.WeatherType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 감정 분석 프로세스 오케스트레이션 서비스
 * 전체 분석 프로세스 조정 및 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionAnalysisOrchestrator {

    private final TimeRangeAnalysisService timeRangeAnalysisService;
    private final EmotionAnalysisService emotionAnalysisService;
    private final EmotionStatisticsService emotionStatisticsService;
    private final EmotionCalculatorService emotionCalculatorService;
    private final CampusTemperatureManager temperatureManager;

    /**
     * 스케줄러용 매시간 감정 분석 실행
     */
    public Map<String, Integer> executeHourlyEmotionAnalysis() {
        AnalysisTimeRange timeRange = timeRangeAnalysisService.calculateScheduledTimeRange();
        return executeEmotionAnalysis(timeRange);
    }

    /**
     * 수동 감정 분석 실행
     */
    public Map<String, Integer> executeManualEmotionAnalysis() {
        AnalysisTimeRange timeRange = timeRangeAnalysisService.calculateManualTimeRange();
        return executeEmotionAnalysis(timeRange);
    }

    /**
     * 오늘 하루 전체 감정 분석 실행
     */
    public Map<String, Integer> executeAllTodaysAnalysis() {
        AnalysisTimeRange timeRange = timeRangeAnalysisService.calculateTodaysAllDayRange();
        return executeEmotionAnalysis(timeRange);
    }

    /**
     * 감정 분석 프로세스 실행 (공통 로직)
     */
    private Map<String, Integer> executeEmotionAnalysis(AnalysisTimeRange timeRange) {
        log.info("감정 분석 프로세스 시작 ({}): {} ~ {}", 
                timeRange.isScheduled() ? "스케줄러" : "수동",
                timeRange.getStartTime(), timeRange.getEndTime());

        // 1. 시간 범위 내 게시글 조회
        List<Content> contents = timeRangeAnalysisService.getContentsByTimeRange(timeRange);
        
        if (contents.isEmpty()) {
            log.info("분석할 게시글이 없습니다. 시간 범위: {} ~ {}", 
                    timeRange.getStartTime(), timeRange.getEndTime());
            return new java.util.HashMap<>();
        }

        // 2. 게시글을 PostData로 변환
        List<PostData> postDataList = contents.stream()
                .map(content -> new PostData(content.getTitle(), content.getContent()))
                .collect(Collectors.toList());

        // 3. GPT-5로 감정 분석
        Map<String, Integer> emotionScores = emotionAnalysisService.analyzeBatchEmotions(postDataList);

        // 4. 통계 데이터 업데이트
        updateEmotionStatistics(contents.size(), timeRange.getStartTime(), emotionScores);

        log.info("감정 분석 프로세스 완료. 분석 게시글 수: {}, 감정 점수: {}", 
                contents.size(), emotionScores);
        
        return emotionScores; // 분석된 최근 점수 반환
    }

    /**
     * 감정 통계 데이터 업데이트
     */
    private void updateEmotionStatistics(int postCount, java.time.LocalDateTime analysisTime, Map<String, Integer> emotionScores) {
        // 1. 기본 통계 업데이트
        emotionStatisticsService.updateDailyAverageScores(emotionScores);
        emotionStatisticsService.updateTotalPostsCount(postCount);
        emotionStatisticsService.updateCumulativeScores(emotionScores);
        emotionStatisticsService.updateHourlyPostsCount(analysisTime, postCount);

        // 2. 날씨와 온도 재계산 및 저장
        updateWeatherAndTemperature();
    }

    /**
     * 감정 날씨와 온도 재계산 및 업데이트
     */
    private void updateWeatherAndTemperature() {
        Map<String, Double> averageScores = emotionStatisticsService.getCurrentDailyAverageScores();
        
        WeatherType weather = emotionCalculatorService.calculateEmotionWeather(averageScores);
        Double temperature = emotionCalculatorService.calculateEmotionTemperature(averageScores);
        temperature = emotionCalculatorService.roundTemperature(temperature);
        
        // 감정 통계 서비스에 날씨와 온도 업데이트
        emotionStatisticsService.updateWeatherAndTemperature(weather.getKoreanName(), temperature);
        
        // 온도 매니저에 감정 분석 결과 전달 (campus_temperature 테이블 기록 및 실시간 최고/최저 업데이트)
        if (temperature != null) {
            temperatureManager.updateTemperatureFromEmotionAnalysis(temperature);
            log.info("감정 분석 결과 온도 매니저 전달 완료 - 온도: {}도, 날씨: {}", temperature, weather.getKoreanName());
        } else {
            log.warn("감정 온도가 null이어서 온도 매니저 전달을 건너뜁니다");
        }
    }
}