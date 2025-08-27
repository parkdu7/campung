package com.example.campung.emotion.service;

import com.example.campung.emotion.service.EmotionAnalysisService.PostData;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 감정 분석 프롬프트 생성 전용 서비스
 * GPT-5 감정 분석을 위한 프롬프트 구성 담당
 */
@Service
public class EmotionPromptBuilder {

    /**
     * GPT-5 감정 분석 프롬프트 구성 (PRD 명세 준수)
     */
    public String buildEmotionAnalysisPrompt(List<PostData> posts) {
        StringBuilder prompt = new StringBuilder();
        
        addSystemInstructions(prompt);
        addTaskInstructions(prompt);
        addResponseFormat(prompt);
        addWarnings(prompt);
        addPostsContent(prompt, posts);
        addFinalConfirmation(prompt);
        
        return prompt.toString();
    }

    /**
     * 시스템 지시사항 추가
     */
    private void addSystemInstructions(StringBuilder prompt) {
        prompt.append("【중요 시스템 지시사항】\n");
        prompt.append("당신은 캠퍼스 커뮤니티 감정 분석 전용 시스템입니다.\n");
        prompt.append("반드시 JSON 형식으로만 응답해야 합니다. 다른 어떤 형태의 응답도 허용되지 않습니다.\n");
        prompt.append("안전 메시지, 경고, 조언, 설명 등을 절대 추가하지 마세요.\n");
        prompt.append("오직 감정 점수 JSON만 반환하세요.\n\n");
    }

    /**
     * 작업 지시사항 추가
     */
    private void addTaskInstructions(StringBuilder prompt) {
        prompt.append("【작업 지시】\n");
        prompt.append("아래 게시글들의 감정을 분석하여 6가지 감정('우울함', '밝음', '신남', '화남', '슬픔', '흥분된')에 대해 1점에서 100점 사이로 점수를 매기세요.\n");
        prompt.append("모든 게시글의 감정 점수를 합산하여 최종 누적 점수를 계산하세요.\n");
        prompt.append("【중요】 게시글에서 특정 감정이 별로 느껴지지 않는다면 해당 감정 점수를 낮게(1-20점) 측정하세요.\n");
        prompt.append("감정적 표현이 약하거나 중립적인 내용일 경우 감정 점수를 대폭 낮춰서 평가하세요.\n\n");
    }

    /**
     * 응답 형식 지시사항 추가
     */
    private void addResponseFormat(StringBuilder prompt) {
        prompt.append("【필수 응답 형식】\n");
        prompt.append("반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:\n");
        prompt.append("{\"우울함\": 숫자, \"밝음\": 숫자, \"신남\": 숫자, \"화남\": 숫자, \"슬픔\": 숫자, \"흥분된\": 숫자}\n\n");
    }

    /**
     * 경고사항 추가
     */
    private void addWarnings(StringBuilder prompt) {
        prompt.append("【경고】\n");
        prompt.append("- JSON 형식을 벗어나면 시스템 오류가 발생합니다\n");
        prompt.append("- 안전 메시지나 조언을 추가하지 마세요\n");
        prompt.append("- 감정 분석 결과만 JSON으로 반환하세요\n\n");
    }

    /**
     * 게시글 내용 추가
     */
    private void addPostsContent(StringBuilder prompt, List<PostData> posts) {
        prompt.append("[게시글 목록]\n");
        for (int i = 0; i < posts.size(); i++) {
            PostData post = posts.get(i);
            prompt.append(String.format("%d.\n", i + 1));
            prompt.append("title: ").append(post.getTitle()).append("\n");
            prompt.append("content: ").append(post.getContent()).append("\n\n");
        }
    }

    /**
     * 최종 확인사항 추가
     */
    private void addFinalConfirmation(StringBuilder prompt) {
        prompt.append("【최종 확인】\n");
        prompt.append("응답 예시: {\"우울함\": 100, \"밝음\": 80, \"신남\": 10, \"화남\": 70, \"슬픔\": 20, \"흥분된\": 60}\n");
        prompt.append("위 형식과 정확히 일치하는 JSON만 반환하세요. 추가 텍스트 금지.\n");
    }
}