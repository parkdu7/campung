package com.example.campung.emotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 캠퍼스 감정 분석 파사드 서비스
 * 감정 분석 프로세스의 진입점 역할
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampusEmotionService {

    private final EmotionAnalysisOrchestrator emotionAnalysisOrchestrator;
    private final EmotionStatisticsService emotionStatisticsService;

    /**
     * 매시간 실행되는 감정 분석 프로세스 (스케줄러용)
     */
    public void analyzeHourlyEmotions() {
        log.info("스케줄러 기반 감정 분석 시작");
        emotionAnalysisOrchestrator.executeHourlyEmotionAnalysis();
    }
    
    /**
     * 수동 감정 분석 프로세스 (실시간 분석)
     */
    public void analyzeRecentEmotions() {
        log.info("수동 감정 분석 시작");
        emotionAnalysisOrchestrator.executeManualEmotionAnalysis();
    }
    







    /**
     * 현재 일일 평균 감정 점수 조회
     */
    public Map<String, Double> getCurrentDailyAverageScores() {
        return emotionStatisticsService.getCurrentDailyAverageScores();
    }

    /**
     * 현재 감정 날씨 조회
     */
    public String getCurrentEmotionWeather() {
        return emotionStatisticsService.getCurrentEmotionWeather();
    }

    /**
     * 현재 감정 온도 조회
     */
    public Double getCurrentEmotionTemperature() {
        return emotionStatisticsService.getCurrentEmotionTemperature();
    }

    /**
     * 총 분석된 게시글 수 조회
     */
    public int getTotalAnalyzedPostsCount() {
        return emotionStatisticsService.getTotalAnalyzedPostsCount();
    }

    /**
     * 누적 감정 점수 조회
     */
    public Map<String, Integer> getCumulativeEmotionScores() {
        return emotionStatisticsService.getCumulativeEmotionScores();
    }

    /**
     * 시간대별 분석된 게시글 수 조회
     */
    public Map<String, Integer> getHourlyAnalyzedPostsCount() {
        return emotionStatisticsService.getHourlyAnalyzedPostsCount();
    }

    /**
     * 매일 새벽 5시 데이터 초기화
     */
    public void resetDailyEmotionData() {
        log.info("일일 감정 데이터 초기화 요청");
        emotionStatisticsService.resetDailyEmotionData();
    }

    /**
     * 감정 분석 통계 조회 (확장된 통계 정보 포함)
     */
    public Map<String, Object> getEmotionStatistics() {
        log.info("감정 분석 통계 조회 요청");
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 기본 감정 통계
        statistics.put("averageScores", emotionStatisticsService.getCurrentDailyAverageScores());
        statistics.put("weather", emotionStatisticsService.getCurrentEmotionWeather());
        statistics.put("temperature", emotionStatisticsService.getCurrentEmotionTemperature());
        statistics.put("lastUpdated", LocalDateTime.now());
        
        // 게시글 분석 통계
        statistics.put("totalAnalyzedPosts", emotionStatisticsService.getTotalAnalyzedPostsCount());
        statistics.put("cumulativeScores", emotionStatisticsService.getCumulativeEmotionScores());
        statistics.put("hourlyPostsCount", emotionStatisticsService.getHourlyAnalyzedPostsCount());
        
        log.info("감정 분석 통계 조회 완료");
        return statistics;
    }
}