package com.example.campung.emotion.scheduler;

import com.example.campung.emotion.service.CampusEmotionService;
import com.example.campung.emotion.service.CampusTemperatureManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 감정 분석 및 온도 관리 통합 스케줄러
 * 매시간 정각에 감정 분석 + 온도 조정 실행, 매일 새벽 5시에 데이터 초기화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmotionAnalysisScheduler {

    private final CampusEmotionService campusEmotionService;
    private final CampusTemperatureManager temperatureManager;

    /**
     * 매시간 정각에 통합 작업 실행 (감정 분석 + 온도 조정)
     * cron: "초 분 시 일 월 요일"
     * 0 0 * * * * = 매시간 0분 0초
     */
    @Scheduled(cron = "0 0 * * * *")
    public void executeHourlyTasks() {
        log.info("매시간 통합 작업 시작 (감정 분석 + 온도 조정)");
        
        // 1. 기존 감정 분석 (감정 기반 온도 계산)
        campusEmotionService.analyzeHourlyEmotions();
        
        // 2. 게시글 활동 기반 온도 조정 (신규)
        temperatureManager.adjustHourlyTemperature();
        
        log.info("매시간 통합 작업 완료");
    }

    /**
     * 매시 30분 자연적 온도 안정화
     * cron: "0 30 * * * *" = 매시간 30분 0초
     */
    @Scheduled(cron = "0 30 * * * *")
    public void naturalTemperatureStabilization() {
        log.info("자연적 온도 안정화 시작");
        
        temperatureManager.naturalTemperatureRecovery();
        
        log.info("자연적 온도 안정화 완료");
    }
    
    /**
     * 매일 밤 10시 다음날 예측 및 보정
     * cron: "0 0 22 * * *" = 매일 오후 10시 0분 0초
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void preventiveTemperatureAdjustment() {
        log.info("다음날 아침 온도 예측 및 사전 보정 시작");
        
        temperatureManager.predictAndAdjustForMorning();
        
        log.info("다음날 아침 온도 예측 및 사전 보정 완료");
    }
    
    /**
     * 매일 오전 6시 최저 온도 보장
     * cron: "0 0 6 * * *" = 매일 오전 6시 0분 0초
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void ensureMorningTemperature() {
        log.info("아침 최저 온도 보장 시작");
        
        temperatureManager.ensureMinimumMorningTemperature();
        
        log.info("아침 최저 온도 보장 완료");
    }
    
    /**
     * 매일 새벽 5시에 데이터 초기화 및 저장
     * cron: "0 0 5 * * *" = 매일 오전 5시 0분 0초
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void resetDailyData() {
        log.info("일일 데이터 초기화 및 저장 시작");
        
        // 1. 전날 캠퍼스 데이터 저장
        temperatureManager.saveDailyCampusData();
        
        // 2. 기존 감정 데이터 초기화
        campusEmotionService.resetDailyEmotionData();
        
        log.info("일일 데이터 초기화 및 저장 완료");
    }
}