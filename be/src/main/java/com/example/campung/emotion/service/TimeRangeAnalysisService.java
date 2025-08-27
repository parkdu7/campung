package com.example.campung.emotion.service;

import com.example.campung.content.repository.ContentRepository;
import com.example.campung.entity.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시간 범위별 게시글 분석 서비스
 * 시간 범위 계산 및 게시글 조회 전용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimeRangeAnalysisService {

    private final ContentRepository contentRepository;

    /**
     * 분석 시간 범위 정보
     */
    public static class AnalysisTimeRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final boolean isScheduled;

        public AnalysisTimeRange(LocalDateTime startTime, LocalDateTime endTime, boolean isScheduled) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.isScheduled = isScheduled;
        }

        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public boolean isScheduled() { return isScheduled; }
    }

    /**
     * 스케줄러용 시간 범위 계산 (직전 정시간)
     */
    public AnalysisTimeRange calculateScheduledTimeRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusHours(1).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = now.withMinute(0).withSecond(0).withNano(0);
        
        return new AnalysisTimeRange(startTime, endTime, true);
    }

    /**
     * 수동 분석용 시간 범위 계산 (현재 시간대)
     */
    public AnalysisTimeRange calculateManualTimeRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = now;
        
        return new AnalysisTimeRange(startTime, endTime, false);
    }

    /**
     * 시간 범위 내 게시글 조회
     */
    public List<Content> getContentsByTimeRange(AnalysisTimeRange timeRange) {
        log.info("게시글 조회 시작 ({}): {} ~ {}", 
                timeRange.isScheduled() ? "스케줄러" : "수동", 
                timeRange.getStartTime(), timeRange.getEndTime());
                
        List<Content> contents = contentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                timeRange.getStartTime(), timeRange.getEndTime());
        
        log.info("조회된 게시글 수: {}", contents.size());
        return contents;
    }

    /**
     * 게시글 존재 여부 확인
     */
    public boolean hasContentInTimeRange(AnalysisTimeRange timeRange) {
        List<Content> contents = getContentsByTimeRange(timeRange);
        return !contents.isEmpty();
    }
}