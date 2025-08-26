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
import java.util.HashMap;
import java.util.Map;

/**
 * 공식 GPT-5 API 가이드 완전 준수 서비스
 * 참조: https://api.openai.com/v1/responses
 * 
 * 지원 모델: gpt-5 only
 * 파라미터: reasoning.effort, text.verbosity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GPT5FallbackService {

    @Qualifier("openaiWebClient")
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * GPT-5 Responses API 폴백 호출
     */
    public String generateSummaryWithFallback(String landmarkName, String postsContent, int radius) {
        return generateSummaryWithModel("gpt-5", landmarkName, postsContent, radius, "medium", "low");
    }
    
    /**
     * 공식 가이드의 reasoning_effort 및 verbosity 파라미터 지원
     */
    public String generateSummaryWithModel(String model, String landmarkName, String postsContent, 
                                         int radius, String verbosity, String reasoningEffort) {
        try {
            // GPT-5 Chat Completions API 사용 (공식 가이드 준수)
            String result = callGPT5ResponsesAPI(landmarkName, postsContent, radius, verbosity, reasoningEffort);
            if (result != null && !result.contains("요약을 생성할 수 없습니다")) {
                log.info("gpt-5 Chat Completions API 성공");
                return result;
            }
            
            log.error("gpt-5 Chat Completions API 실패");
            return landmarkName + " 주변 분위기를 분석할 수 없습니다.";
            
        } catch (Exception e) {
            log.error("gpt-5 Chat Completions API 호출 실패: {}", e.getMessage(), e);
            return landmarkName + " 주변 분위기를 분석하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 공식 GPT-5 Chat Completions API 호출 (가이드 완전 준수)
     */
    private String callGPT5ResponsesAPI(String landmarkName, String postsContent, 
                                       int radius, String verbosity, String reasoningEffort) {
        try {
            // verbosity에 따른 응답 길이 조정
            String lengthInstruction = getVerbosityInstruction(verbosity);
            
            String inputText = String.format(
                "당신은 대학 캠퍼스 커뮤니티의 분위기를 분석하는 전문 AI입니다.\n\n" +
                "다음은 %s 주변 %dm 이내의 최근 게시글들입니다.\n" +
                "캠퍼스 학생들이 이 장소 주변에서 어떤 분위기인지 친근하고 유익한 톤으로 %s 요약해주세요.\n\n" +
                "주의사항:\n" +
                "- 광고성, 스팸성, 부적절한 내용은 제외하고 요약해 주세요\n" +
                "- 학생들의 실제 캠퍼스 생활과 관련된 내용만 반영해 주세요\n" +
                "- 감정이나 키워드를 중심으로 현재 분위기를 전달해 주세요\n" +
                "- 마크다운 문법(**굵게**, \\n 개행 등)을 사용하지 말고 일반 텍스트로만 작성해 주세요\n\n" +
                "게시글 내용:\n%s",
                landmarkName, radius, lengthInstruction, postsContent
            );

            // 공식 GPT-5 Chat Completions API 요청 구조 (가이드 준수)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-5");
            requestBody.put("messages", java.util.Arrays.asList(
                Map.of("role", "user", "content", inputText)
            ));
            
            // 공식 가이드의 reasoning_effort 파라미터 (Chat Completions 방식)
            if (reasoningEffort != null) {
                requestBody.put("reasoning_effort", reasoningEffort);
            }
            
            // 공식 가이드의 verbosity 파라미터 (Chat Completions 방식)
            if (verbosity != null) {
                requestBody.put("verbosity", verbosity);
            }

            log.info("gpt-5 Chat Completions API 호출 시작 (reasoning_effort: {}, verbosity: {})", 
                    reasoningEffort, verbosity);

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .onErrorResume(throwable -> {
                        log.error("gpt-5 Chat Completions API 호출 실패: {}", throwable.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null) {
                return extractChatCompletionsText(response);
            }
            
            return null;

        } catch (Exception e) {
            log.error("gpt-5 Responses API 호출 중 예외: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verbosity 지시사항 반환
     */
    private String getVerbosityInstruction(String verbosity) {
        if (verbosity == null) return "3-4줄로";
        
        return switch (verbosity.toLowerCase()) {
            case "low" -> "2-3줄로 간단히";
            case "high" -> "5-6줄로 자세히";
            default -> "3-4줄로";
        };
    }

    /**
     * GPT-5 Chat Completions API 응답에서 텍스트 추출
     */
    private String extractChatCompletionsText(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // Chat Completions 표준 응답 형식
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
                JsonNode firstChoice = jsonNode.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String content = firstChoice.get("message").get("content").asText();
                    return cleanMarkdownSyntax(content);
                }
            }
            
            log.warn("GPT-5 Chat Completions 응답에서 content를 찾을 수 없음");
            return "요약을 생성할 수 없습니다.";
            
        } catch (Exception e) {
            log.error("GPT-5 Chat Completions 응답 파싱 중 오류: {}", e.getMessage());
            return "응답 처리 중 오류가 발생했습니다.";
        }
    }

    /**
     * 마크다운 문법 제거
     */
    private String cleanMarkdownSyntax(String text) {
        if (text == null) return null;
        
        return text
            .replaceAll("\\*\\*(.*?)\\*\\*", "$1")  // **굵게** → 굵게
            .replaceAll("\\*(.*?)\\*", "$1")        // *기울임* → 기울임
            .replaceAll("\\n", " ")                 // 개행 제거
            .replaceAll("\\s+", " ")                // 연속 공백 정리
            .trim();
    }
}