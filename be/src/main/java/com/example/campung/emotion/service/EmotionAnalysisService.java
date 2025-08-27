package com.example.campung.emotion.service;

import com.example.campung.emotion.service.PostBatchProcessor.BatchEmotionAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 감정 분석 오케스트레이션 서비스
 * 각 전문 서비스들을 조합하여 감정 분석 프로세스 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionAnalysisService implements BatchEmotionAnalyzer {

    private final GPT5ApiService gpt5ApiService;
    private final EmotionPromptBuilder promptBuilder;
    private final EmotionResponseParser responseParser;
    private final PostBatchProcessor batchProcessor;

    /**
     * 게시글 배치를 GPT-5로 감정 분석 (진입점)
     */
    public Map<String, Integer> analyzeBatchEmotions(List<PostData> posts) {
        log.info("감정 분석 배치 처리 시작: {}개 게시글", posts.size());
        
        Map<String, Integer> result = batchProcessor.processBatchEmotions(posts, this);
        
        log.info("감정 분석 배치 처리 완료: {}", result);
        return result;
    }

    /**
     * 단일 배치 감정 분석 (BatchEmotionAnalyzer 인터페이스 구현)
     */
    @Override
    public Map<String, Integer> analyzeSingleBatch(List<PostData> posts) {
        log.info("단일 배치 감정 분석 시작: {}개 게시글", posts.size());
        
        String prompt = promptBuilder.buildEmotionAnalysisPrompt(posts);
        String response = gpt5ApiService.callChatCompletions(prompt);
        
        if (response != null) {
            Map<String, Integer> result = responseParser.parseEmotionResponse(response);
            log.info("단일 배치 감정 분석 완료: {}", result);
            return result;
        }
        
        log.warn("GPT-5 응답이 null, 빈 감정 맵 반환");
        return responseParser.parseEmotionResponse("");
    }

    /**
     * 게시글 데이터 DTO
     */
    public static class PostData {
        private String title;
        private String content;

        public PostData(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}