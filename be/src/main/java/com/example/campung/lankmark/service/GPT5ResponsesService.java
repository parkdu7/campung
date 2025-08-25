package com.example.campung.lankmark.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GPT5ResponsesService {

    @Qualifier("openaiWebClient")
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * GPT-5 Responses API 호출
     * 공식 GPT-5 API를 직접 호출하여 최신 기능 활용
     */
    public String callGPT5(String systemPrompt, String userPrompt) {
        return callGPT5(systemPrompt, userPrompt, "medium", "medium");
    }

    /**
     * GPT-5 Responses API 호출 (파라미터 제어)
     */
    public String callGPT5(String systemPrompt, String userPrompt, String reasoningEffort, String verbosity) {
        try {
            // GPT-5 Responses API 요청 body 구성
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-5",
                "input", formatMessages(systemPrompt, userPrompt),
                "reasoning", Map.of("effort", reasoningEffort), // minimal, medium, high
                "text", Map.of("verbosity", verbosity),         // low, medium, high
                "max_completion_tokens", 300
            );

            log.info("GPT-5 API 호출 시작 - reasoning: {}, verbosity: {}", reasoningEffort, verbosity);

            // WebClient로 GPT-5 Responses API 호출
            String response = webClient.post()
                    .uri("/responses")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .onErrorResume(throwable -> {
                        log.error("GPT-5 API 호출 실패: {}", throwable.getMessage());
                        return Mono.just(createErrorResponse(throwable.getMessage()));
                    })
                    .block();

            log.info("GPT-5 API 응답 수신 완료");
            return extractResponseText(response);

        } catch (Exception e) {
            log.error("GPT-5 API 호출 중 예외 발생: {}", e.getMessage(), e);
            return "요약 생성 중 오류가 발생했습니다.";
        }
    }

    /**
     * 캠퍼스 커뮤니티 요약 전용 GPT-5 호출 (폴백 포함)
     */
    public String generateCampusSummary(String landmarkName, String postsContent, int radius) {
        // GPT5FallbackService를 통해 안전한 호출
        GPT5FallbackService fallbackService = new GPT5FallbackService(webClient, objectMapper);
        return fallbackService.generateSummaryWithFallback(landmarkName, postsContent, radius);
    }

    /**
     * 빠른 응답이 필요한 경우 (minimal reasoning + low verbosity)
     */
    public String generateQuickSummary(String landmarkName, String postsContent, int radius) {
        String systemPrompt = "대학 캠퍼스 커뮤니티 분위기 분석 AI입니다.";
        
        String userPrompt = String.format(
            "%s 주변 %dm 게시글들의 분위기를 2-3줄로 간단히 요약해주세요:\n\n%s",
            landmarkName, radius, postsContent
        );

        return callGPT5(systemPrompt, userPrompt, "minimal", "low");
    }

    private String formatMessages(String systemPrompt, String userPrompt) {
        // GPT-5 Responses API는 단일 input 형태를 선호
        return systemPrompt + "\n\n" + userPrompt;
    }

    private String extractResponseText(String response) {
        try {
            // Jackson을 사용한 JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // GPT-5 Responses API 응답 형식에 따른 파싱
            if (jsonNode.has("output_text")) {
                return jsonNode.get("output_text").asText();
            }
            
            // Chat Completions API 형식 폴백
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
                JsonNode firstChoice = jsonNode.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
            
            log.warn("GPT-5 응답에서 텍스트를 찾을 수 없음: {}", response.substring(0, Math.min(200, response.length())));
            return "요약을 생성할 수 없습니다.";
            
        } catch (Exception e) {
            log.error("GPT-5 응답 파싱 중 오류: {}", e.getMessage());
            return "응답 처리 중 오류가 발생했습니다.";
        }
    }

    private String createErrorResponse(String errorMessage) {
        return String.format("{\"error\":{\"message\":\"%s\"}}", errorMessage);
    }
}