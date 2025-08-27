package com.example.campung.emotion.scheduler;

import com.example.campung.emotion.service.CampusEmotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 감정 분석 스케줄러
 * 매시간 정각에 감정 분석 실행, 매일 새벽 5시에 데이터 초기화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmotionAnalysisScheduler {

    private final CampusEmotionService campusEmotionService;

    /**
     * 매시간 정각에 감정 분석 실행
     * cron: "초 분 시 일 월 요일"
     * 0 0 * * * * = 매시간 0분 0초
     */
    @Scheduled(cron = "0 0 * * * *")
    public void analyzeHourlyEmotions() {
        log.info("매시간 감정 분석 스케줄러 시작");
        
        campusEmotionService.analyzeHourlyEmotions();
        
        log.info("매시간 감정 분석 스케줄러 완료");
    }

    /**
     * 매일 새벽 5시에 데이터 초기화
     * cron: "초 분 시 일 월 요일"
     * 0 0 5 * * * = 매일 오전 5시 0분 0초
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void resetDailyEmotionData() {
        log.info("일일 감정 데이터 초기화 스케줄러 시작");
        
        campusEmotionService.resetDailyEmotionData();
        
        log.info("일일 감정 데이터 초기화 스케줄러 완료");
    }
}