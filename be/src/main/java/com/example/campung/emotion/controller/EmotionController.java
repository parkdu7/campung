package com.example.campung.emotion.controller;

import com.example.campung.emotion.dto.EmotionStatisticsResponse;
import com.example.campung.emotion.service.CampusEmotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 감정 분석 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/emotion")
@RequiredArgsConstructor
@Slf4j
public class EmotionController {

    private final CampusEmotionService campusEmotionService;

    /**
     * 현재 감정 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<EmotionStatisticsResponse> getEmotionStatistics() {
        log.info("감정 통계 조회 요청");
        
        Map<String, Object> statistics = campusEmotionService.getEmotionStatistics();
        
        EmotionStatisticsResponse.EmotionStatisticsData data = EmotionStatisticsResponse.EmotionStatisticsData.builder()
                .averageScores((Map<String, Double>) statistics.get("averageScores"))
                .emotionWeather((String) statistics.get("weather"))
                .emotionTemperature((Double) statistics.get("temperature"))
                .lastUpdated(((LocalDateTime) statistics.get("lastUpdated")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .totalAnalyzedPosts((Integer) statistics.get("totalAnalyzedPosts"))
                .hourlyPostsCount((Map<String, Integer>) statistics.get("hourlyPostsCount"))
                .build();

        EmotionStatisticsResponse response = EmotionStatisticsResponse.builder()
                .success(true)
                .data(data)
                .build();

        log.info("감정 통계 조회 성공: 날씨={}, 온도={}", data.getEmotionWeather(), data.getEmotionTemperature());
        return ResponseEntity.ok(response);
    }

    /**
     * 수동 감정 분석 실행
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> manualEmotionAnalysis() {
        log.info("수동 감정 분석 실행 요청");
        
        Map<String, Integer> newAnalysisScores = campusEmotionService.analyzeRecentEmotions();
        
        // 전체 평균 점수 조회
        Map<String, Double> averageScores = campusEmotionService.getCurrentDailyAverageScores();
        String weather = campusEmotionService.getCurrentEmotionWeather();
        Double temperature = campusEmotionService.getCurrentEmotionTemperature();
        
        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("newAnalysisScores", newAnalysisScores);
        analysisResult.put("overallAverageScores", averageScores);
        analysisResult.put("emotionWeather", weather);
        analysisResult.put("emotionTemperature", temperature);
        analysisResult.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "감정 분석이 완료되었습니다");
        response.put("data", analysisResult);
        
        log.info("수동 감정 분석 완료");
        return ResponseEntity.ok(response);
    }

    /**
     * 오늘 하루 전체 감정 분석 실행
     */
    @PostMapping("/analyzeAll")
    public ResponseEntity<Map<String, Object>> analyzeAllTodaysPosts() {
        log.info("오늘 하루 전체 감정 분석 실행 요청");

        Map<String, Integer> newAnalysisScores = campusEmotionService.analyzeAllTodaysEmotions();

        // 전체 평균 점수 조회
        Map<String, Double> averageScores = campusEmotionService.getCurrentDailyAverageScores();
        String weather = campusEmotionService.getCurrentEmotionWeather();
        Double temperature = campusEmotionService.getCurrentEmotionTemperature();
        
        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("newAnalysisScores", newAnalysisScores);
        analysisResult.put("overallAverageScores", averageScores);
        analysisResult.put("emotionWeather", weather);
        analysisResult.put("emotionTemperature", temperature);
        analysisResult.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "오늘 하루 전체 감정 분석이 완료되었습니다");
        response.put("data", analysisResult);

        log.info("오늘 하루 전체 감정 분석 완료");
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 데이터 초기화
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetEmotionData() {
        log.info("감정 데이터 초기화 요청");
        
        campusEmotionService.resetDailyEmotionData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "감정 데이터가 초기화되었습니다");
        response.put("resetTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        log.info("감정 데이터 초기화 완료");
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 감정 날씨만 조회 (간단 조회)
     */
    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> getCurrentWeather() {
        log.info("현재 감정 날씨 조회 요청");
        
        String weather = campusEmotionService.getCurrentEmotionWeather();
        Double temperature = campusEmotionService.getCurrentEmotionTemperature();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "emotionWeather", weather,
                "emotionTemperature", temperature,
                "lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
        
        log.info("감정 날씨 조회 성공: {}°C, {}", temperature, weather);
        return ResponseEntity.ok(response);
    }

    /**
     * 평균 감정 점수만 조회
     */
    @GetMapping("/scores")
    public ResponseEntity<Map<String, Object>> getEmotionScores() {
        log.info("감정 점수 조회 요청");
        
        Map<String, Double> averageScores = campusEmotionService.getCurrentDailyAverageScores();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "averageScores", averageScores,
                "lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
        
        log.info("감정 점수 조회 성공: {}", averageScores);
        return ResponseEntity.ok(response);
    }
}