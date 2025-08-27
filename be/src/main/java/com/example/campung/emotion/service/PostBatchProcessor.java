package com.example.campung.emotion.service;

import com.example.campung.emotion.service.EmotionAnalysisService.PostData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 게시글 배치 처리 전용 서비스
 * 배치 분할, 청크 처리, 결과 집계 담당
 */
@Service
@Slf4j
public class PostBatchProcessor {

    private static final int MAX_BATCH_SIZE = 30;

    /**
     * 게시글 배치를 적절한 크기로 분할하여 처리
     */
    public Map<String, Integer> processBatchEmotions(List<PostData> posts, BatchEmotionAnalyzer analyzer) {
        if (posts.isEmpty()) {
            return createEmptyEmotionMap();
        }

        if (posts.size() <= MAX_BATCH_SIZE) {
            return analyzer.analyzeSingleBatch(posts);
        }

        return processInChunks(posts, analyzer);
    }

    /**
     * 대용량 배치를 청크 단위로 분할 처리
     */
    private Map<String, Integer> processInChunks(List<PostData> posts, BatchEmotionAnalyzer analyzer) {
        Map<String, Integer> totalScores = createEmptyEmotionMap();
        
        int totalChunks = (int) Math.ceil((double) posts.size() / MAX_BATCH_SIZE);
        log.info("대용량 배치 처리 시작: 총 {}개 게시글을 {}개 청크로 분할", posts.size(), totalChunks);
        
        for (int i = 0; i < posts.size(); i += MAX_BATCH_SIZE) {
            int endIndex = Math.min(i + MAX_BATCH_SIZE, posts.size());
            List<PostData> chunk = posts.subList(i, endIndex);
            
            int currentChunk = (i / MAX_BATCH_SIZE) + 1;
            log.info("청크 {}/{} 처리 중 (게시글 {}-{})", currentChunk, totalChunks, i + 1, endIndex);
            
            Map<String, Integer> chunkScores = analyzer.analyzeSingleBatch(chunk);
            aggregateScores(totalScores, chunkScores);
        }
        
        log.info("대용량 배치 처리 완료: 총 {}개 청크 처리됨", totalChunks);
        return totalScores;
    }

    /**
     * 청크별 점수를 전체 점수에 합산
     */
    private void aggregateScores(Map<String, Integer> totalScores, Map<String, Integer> chunkScores) {
        for (String emotion : totalScores.keySet()) {
            int currentTotal = totalScores.get(emotion);
            int chunkScore = chunkScores.getOrDefault(emotion, 0);
            totalScores.put(emotion, currentTotal + chunkScore);
        }
    }

    /**
     * 빈 감정 맵 생성
     */
    private Map<String, Integer> createEmptyEmotionMap() {
        Map<String, Integer> emotions = new HashMap<>();
        emotions.put("우울함", 0);
        emotions.put("밝음", 0);
        emotions.put("신남", 0);
        emotions.put("화남", 0);
        emotions.put("슬픔", 0);
        emotions.put("흥분된", 0);
        return emotions;
    }

    /**
     * 배치 감정 분석 인터페이스
     */
    public interface BatchEmotionAnalyzer {
        Map<String, Integer> analyzeSingleBatch(List<PostData> posts);
    }
}