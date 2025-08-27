package com.example.campung.emotion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.campung.global.exception.EmotionAnalysisException;
import com.example.campung.global.enums.EmotionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * GPT-5 감정 분석 응답 파싱 전용 서비스
 * 응답 파싱, 안전 응답 감지, 점수 검증 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * GPT-5 응답을 감정 점수 맵으로 파싱
     */
    public Map<String, Integer> parseEmotionResponse(String response) {
        if (isSafetyResponse(response)) {
            log.warn("GPT-5가 안전 응답을 반환했습니다. 부정적 감정 점수로 처리합니다.");
            return createSafetyEmotionMap();
        }
        
        String jsonPart = extractJsonFromResponse(response);
        
        if (jsonPart == null) {
            log.warn("응답에서 JSON을 찾을 수 없음: {}", response);
            return createEmptyEmotionMap();
        }

        return parseEmotionScores(jsonPart);
    }

    /**
     * JSON 문자열을 감정 점수로 파싱
     */
    private Map<String, Integer> parseEmotionScores(String jsonString) {
        JsonNode jsonNode = parseJsonSafely(jsonString, "감정 분석 결과");
        Map<String, Integer> emotions = new HashMap<>();
        
        for (EmotionType emotionType : EmotionType.values()) {
            String emotionKey = emotionType.getKoreanName();
            if (jsonNode.has(emotionKey)) {
                int score = jsonNode.get(emotionKey).asInt();
                score = validateAndNormalizeScore(score);
                emotions.put(emotionKey, score);
            } else {
                emotions.put(emotionKey, 50);
            }
        }
        
        log.info("감정 분석 완료: {}", emotions);
        return emotions;
    }

    /**
     * 응답에서 JSON 부분 추출
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        
        return null;
    }

    /**
     * 안전 응답인지 확인
     */
    private boolean isSafetyResponse(String response) {
        if (response == null) return false;
        
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("자살") || 
               lowerResponse.contains("위험") ||
               lowerResponse.contains("상담") ||
               lowerResponse.contains("도움") ||
               lowerResponse.contains("112") ||
               lowerResponse.contains("119") ||
               lowerResponse.contains("1393") ||
               lowerResponse.contains("안전") ||
               lowerResponse.contains("걱정");
    }

    /**
     * 빈 감정 맵 생성 (모든 감정 50점 기본값)
     */
    private Map<String, Integer> createEmptyEmotionMap() {
        Map<String, Integer> emotions = new HashMap<>();
        
        for (EmotionType emotionType : EmotionType.values()) {
            emotions.put(emotionType.getKoreanName(), 50);
        }
        
        return emotions;
    }
    
    /**
     * 안전 응답 시 사용할 감정 맵 (부정적 감정 높음)
     */
    private Map<String, Integer> createSafetyEmotionMap() {
        Map<String, Integer> emotions = new HashMap<>();
        
        emotions.put(EmotionType.DEPRESSION.getKoreanName(), 80);
        emotions.put(EmotionType.SADNESS.getKoreanName(), 75);
        emotions.put(EmotionType.BRIGHTNESS.getKoreanName(), 20);
        emotions.put(EmotionType.EXCITEMENT.getKoreanName(), 15);
        emotions.put(EmotionType.ANGER.getKoreanName(), 40);
        emotions.put(EmotionType.THRILLED.getKoreanName(), 10);
        
        return emotions;
    }

    /**
     * 점수 범위 검증 및 정규화 (1-100)
     */
    private int validateAndNormalizeScore(int score) {
        return Math.max(1, Math.min(100, score));
    }

    /**
     * JSON 안전 파싱
     */
    private JsonNode parseJsonSafely(String jsonString, String context) {
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            log.error("{} JSON 파싱 실패: {}", context, e.getMessage());
            throw new EmotionAnalysisException(context + " JSON 파싱에 실패했습니다", e);
        }
        return jsonNode;
    }
}