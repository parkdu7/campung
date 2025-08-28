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
    private final CampusTemperatureManager temperatureManager;

    /**
     * 매시간 실행되는 감정 분석 프로세스 (스케줄러용)
     */
    public void analyzeHourlyEmotions() {
        log.info("스케줄러 기반 감정 분석 시작");
        
        // 감정 분석 실행
        emotionAnalysisOrchestrator.executeHourlyEmotionAnalysis();
        
        // 감정 분석 결과를 온도 매니저에 전달
        updateTemperatureFromEmotionAnalysis();
        
        log.info("스케줄러 기반 감정 분석 완료");
    }
    
    /**
     * 수동 감정 분석 프로세스 (실시간 분석)
     */
    public Map<String, Integer> analyzeRecentEmotions() {
        log.info("수동 감정 분석 시작");
        return emotionAnalysisOrchestrator.executeManualEmotionAnalysis();
    }

    /**
     * 오늘 하루 전체 감정 분석 프로세스
     */
    public Map<String, Integer> analyzeAllTodaysEmotions() {
        log.info("오늘 하루 전체 감정 분석 시작");
        return emotionAnalysisOrchestrator.executeAllTodaysAnalysis();
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
     * 감정 분석 결과를 온도 매니저에 전달하는 내부 메서드
     */
    private void updateTemperatureFromEmotionAnalysis() {
        // 현재 감정 평균 점수 조회
        Map<String, Double> averageScores = getCurrentDailyAverageScores();
        
        if (averageScores == null || averageScores.isEmpty()) {
            log.warn("감정 평균 점수가 없어 온도 전달을 건너뜁니다");
            return;
        }
        
        // 현재 감정 온도 조회
        Double emotionTemperature = getCurrentEmotionTemperature();
        
        if (emotionTemperature == null) {
            log.warn("감정 온도가 없어 온도 전달을 건너뜁니다");
            return;
        }
        
        // 온도 매니저에 감정 기반 온도 설정
        temperatureManager.setBaseEmotionTemperature(emotionTemperature);
        
        log.info("감정 분석 결과를 온도 매니저에 전달 완료: {}도 (감정 점수: {})", 
                emotionTemperature, averageScores);
    }
    
    /**
     * 외부에서 호출 가능한 감정 분석 결과 온도 매니저 전달 메서드
     */
    public void passEmotionToTemperatureManager(Map<String, Double> averageScores) {
        if (averageScores == null || averageScores.isEmpty()) {
            log.warn("감정 평균 점수가 없어 온도 전달을 건너뜁니다");
            return;
        }
        
        // 현재 감정 온도 조회
        Double emotionTemperature = getCurrentEmotionTemperature();
        
        if (emotionTemperature == null) {
            log.warn("감정 온도가 없어 온도 전달을 건너뜁니다");
            return;
        }
        
        // 온도 매니저에 감정 기반 온도 설정
        temperatureManager.setBaseEmotionTemperature(emotionTemperature);
        
        log.info("외부 호출로 감정 분석 결과를 온도 매니저에 전달 완료: {}도 (감정 점수: {})", 
                emotionTemperature, averageScores);
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
        statistics.put("hourlyPostsCount", emotionStatisticsService.getHourlyAnalyzedPostsCount());
        
        log.info("감정 분석 통계 조회 완료");
        return statistics;
    }
}