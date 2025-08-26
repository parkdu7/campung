package com.example.campung.lankmark.scheduler;

import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.lankmark.repository.LandmarkRepository;
import com.example.campung.lankmark.service.LandmarkPostCollectionService;
import com.example.campung.lankmark.service.LandmarkSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LandmarkSummaryScheduler {

    private final LandmarkRepository landmarkRepository;
    private final LandmarkSummaryService landmarkSummaryService;
    private final LandmarkPostCollectionService postCollectionService;

    /**
     * 1시간마다 모든 랜드마크의 요약을 자동 생성
     * cron = "0 0 * * * *" : 매시 정각에 실행 (1시간 간격)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void generateAllLandmarkSummaries() {
        log.info("=== 랜드마크 요약 자동 생성 스케줄러 시작 ===");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<Landmark> allLandmarks = landmarkRepository.findAll();
            
            if (allLandmarks.isEmpty()) {
                log.info("생성할 랜드마크가 없습니다.");
                return;
            }
            
            log.info("총 {}개 랜드마크 요약 생성 시작", allLandmarks.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Landmark landmark : allLandmarks) {
                try {
                    generateSummaryForLandmark(landmark);
                    successCount++;
                    
                    // API 호출 간격 조절 (GPT API rate limit 고려)
                    Thread.sleep(2000); // 2초 대기
                    
                } catch (Exception e) {
                    log.error("랜드마크 {} 요약 생성 실패: {}", landmark.getName(), e.getMessage(), e);
                    failCount++;
                }
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            log.info("=== 랜드마크 요약 자동 생성 완료 === 성공: {}, 실패: {}, 소요시간: {}초", 
                    successCount, failCount, 
                    java.time.Duration.between(startTime, endTime).getSeconds());
                    
        } catch (Exception e) {
            log.error("랜드마크 요약 스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 개별 랜드마크의 요약을 생성
     */
    private void generateSummaryForLandmark(Landmark landmark) {
        log.debug("랜드마크 {} 요약 생성 시작", landmark.getName());
        
        try {
            // 랜드마크 주변 게시글 수집
            int radius = landmark.getCategory().getDefaultRadius();
            List<LandmarkSummaryService.PostData> posts = postCollectionService.collectPostsAroundLandmark(landmark);
            
            if (posts.isEmpty()) {
                log.debug("랜드마크 {} 주변에 분석할 게시글이 없습니다.", landmark.getName());
                return;
            }
            
            // 요약 생성 (캐시 무시하고 새로 생성)
            landmarkSummaryService.clearCache(landmark.getId()); // 기존 캐시 삭제
            String summary = landmarkSummaryService.generateSummary(
                landmark.getId(),
                landmark.getName(),
                posts,
                radius
            );
            
            // 데이터베이스에 요약 저장
            landmark.updateSummary(summary);
            landmarkRepository.save(landmark);
            
            log.debug("랜드마크 {} 요약 생성 및 저장 완료", landmark.getName());
            
        } catch (Exception e) {
            log.error("랜드마크 {} 요약 생성 중 오류: {}", landmark.getName(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 매일 새벽 4시에 요약 데이터 초기화 (선택사항)
     * cron = "0 0 4 * * *" : 매일 오전 4시에 실행
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void initializeDailySummaries() {
        log.info("=== 일일 랜드마크 요약 데이터 초기화 시작 ===");
        
        try {
            List<Landmark> allLandmarks = landmarkRepository.findAll();
            
            for (Landmark landmark : allLandmarks) {
                try {
                    // Redis 캐시 초기화
                    landmarkSummaryService.clearCache(landmark.getId());
                    
                    // DB의 요약 데이터 초기화 (선택사항)
                    landmark.updateSummary(null);
                    landmarkRepository.save(landmark);
                    
                } catch (Exception e) {
                    log.error("랜드마크 {} 초기화 실패: {}", landmark.getName(), e.getMessage());
                }
            }
            
            log.info("=== 일일 랜드마크 요약 데이터 초기화 완료 ===");
            
        } catch (Exception e) {
            log.error("랜드마크 요약 초기화 스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}