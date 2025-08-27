package com.example.campung.emotion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.campung.global.exception.GPT5ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GPT-5 API 호출 전용 서비스
 * API 통신 및 토큰 사용량 로깅 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GPT5ApiService {

    @Qualifier("openaiWebClient")
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * GPT-5 Chat Completions API 호출
     */
    public String callChatCompletions(String prompt) {
        Map<String, Object> requestBody = createRequestBody(prompt);
        
        log.info("GPT-5 API 호출 시작 (프롬프트 길이: {}자)", prompt.length());

        String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(throwable -> {
                    log.error("GPT-5 API 호출 실패: {}", throwable.getMessage());
                    throw new GPT5ServiceException("GPT-5 API 호출에 실패했습니다: " + throwable.getMessage(), throwable);
                })
                .block();

        if (response != null) {
            log.info("GPT-5 API 호출 성공");
            logTokenUsage(response);
            return extractContentFromResponse(response);
        }

        return null;
    }

    /**
     * 요청 바디 생성
     */
    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-5-mini");
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("reasoning_effort", "medium");
        requestBody.put("verbosity", "low");
        
        return requestBody;
    }

    /**
     * Chat Completions 응답에서 content 추출
     */
    private String extractContentFromResponse(String response) {
        JsonNode jsonNode = parseJsonResponse(response, "GPT-5 응답");
        
        if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
            JsonNode firstChoice = jsonNode.get("choices").get(0);
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                return firstChoice.get("message").get("content").asText();
            }
        }
        
        log.warn("GPT-5 응답에서 content를 찾을 수 없음");
        throw new GPT5ServiceException("GPT-5 응답에서 content를 찾을 수 없습니다");
    }

    /**
     * GPT API 응답에서 토큰 사용량 로깅
     */
    private void logTokenUsage(String response) {
        JsonNode jsonNode = parseJsonResponse(response, "토큰 사용량");
        
        if (jsonNode != null && jsonNode.has("usage")) {
            JsonNode usage = jsonNode.get("usage");
            
            int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
            int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
            int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;
            
            // gpt-5-mini 가격 계산 (2025년 기준)
            // Input: $0.25/1M tokens, Output: $2.00/1M tokens
            double inputCost = (promptTokens * 0.25) / 1_000_000;
            double outputCost = (completionTokens * 2.00) / 1_000_000;
            double totalCost = inputCost + outputCost;
            
            log.info("토큰 사용량 - Input: {}, Output: {}, Total: {}, 예상 비용: ${:.6f}", 
                    promptTokens, completionTokens, totalTokens, totalCost);
                    
            // 상세 비용 분석
            if (totalTokens > 0) {
                log.info("비용 상세 - Input: ${:.6f}, Output: ${:.6f}, Total: ${:.6f}", 
                        inputCost, outputCost, totalCost);
            }
        } else {
            log.warn("API 응답에 토큰 사용량 정보가 없습니다");
        }
    }

    /**
     * JSON 응답 파싱 공통 메소드
     */
    private JsonNode parseJsonResponse(String response, String context) {
        if (response == null) {
            log.error("{} JSON 파싱 실패: 응답이 null입니다", context);
            throw new GPT5ServiceException(context + " JSON 파싱에 실패했습니다");
        }
        
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("{} JSON 파싱 실패: {}", context, e.getMessage());
            throw new GPT5ServiceException(context + " JSON 파싱에 실패했습니다", e);
        }
        
        if (jsonNode == null) {
            log.error("{} JSON 파싱 실패: 파싱 결과가 null입니다", context);
            throw new GPT5ServiceException(context + " JSON 파싱에 실패했습니다");
        }
        
        return jsonNode;
    }
}