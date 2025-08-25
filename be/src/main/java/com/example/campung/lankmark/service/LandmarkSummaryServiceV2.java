package com.example.campung.lankmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandmarkSummaryServiceV2 {

    private final GPT5ResponsesService gpt5ResponsesService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String REDIS_KEY_PREFIX = "landmark:summary:v2:";
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * GPT-5 파라미터를 제어할 수 있는 고급 요약 생성
     */
    public String generateAdvancedSummary(Long landmarkId, String landmarkName, 
                                         List<LandmarkSummaryService.PostData> posts, 
                                         int radius, String reasoningEffort, String verbosity) {
        
        // Redis 캐시 키에 파라미터 포함 (각 설정별로 캐싱)
        String cacheKey = String.format("%s%d:%s:%s", REDIS_KEY_PREFIX, landmarkId, reasoningEffort, verbosity);
        String cachedSummary = (String) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedSummary != null) {
            log.info("랜드마크 {} 요약 캐시에서 반환 (reasoning: {}, verbosity: {})", 
                    landmarkId, reasoningEffort, verbosity);
            return cachedSummary;
        }

        // 데이터 품질 필터링
        List<LandmarkSummaryService.PostData> filteredPosts = filterPostsForQuality(posts);
        
        if (filteredPosts.isEmpty()) {
            return landmarkName + " 주변에 분석할 게시글이 없습니다.";
        }

        // GPT-5 파라미터에 따른 맞춤형 요약 생성
        String summary = generateCustomSummary(landmarkName, filteredPosts, radius, reasoningEffort, verbosity);
        
        // Redis에 캐싱 (30분 TTL)
        redisTemplate.opsForValue().set(cacheKey, summary, Duration.ofMinutes(CACHE_TTL_MINUTES));
        
        log.info("랜드마크 {} 고급 요약 생성 완료 및 캐싱 (reasoning: {}, verbosity: {})", 
                landmarkId, reasoningEffort, verbosity);
        return summary;
    }

    private String generateCustomSummary(String landmarkName, List<LandmarkSummaryService.PostData> posts, 
                                       int radius, String reasoningEffort, String verbosity) {
        try {
            // 게시글 목록을 텍스트로 포맷팅
            StringBuilder postsText = new StringBuilder();
            for (LandmarkSummaryService.PostData post : posts) {
                postsText.append("제목: ").append(post.getTitle()).append("\n");
                postsText.append("내용: ").append(post.getContents()).append("\n\n");
            }

            log.info("GPT-5 고급 요약 생성 시작: {} ({}개 게시글, reasoning: {}, verbosity: {})", 
                    landmarkName, posts.size(), reasoningEffort, verbosity);

            // reasoning effort와 verbosity에 따른 적절한 시스템 프롬프트 선택
            String systemPrompt = getSystemPrompt(reasoningEffort, verbosity);
            String userPrompt = getUserPrompt(landmarkName, postsText.toString(), radius, verbosity);

            // GPT-5 Responses API 호출 (파라미터 제어)
            String summary = gpt5ResponsesService.callGPT5(systemPrompt, userPrompt, reasoningEffort, verbosity);
            
            if (summary != null && !summary.trim().isEmpty()) {
                log.info("GPT-5 고급 요약 생성 성공 ({}자): {}", 
                        summary.length(), summary.substring(0, Math.min(50, summary.length())) + "...");
                return summary.trim();
            } else {
                log.error("GPT-5에서 빈 응답 반환");
                return landmarkName + " 주변 분위기를 분석할 수 없습니다.";
            }
            
        } catch (Exception e) {
            log.error("GPT-5 고급 요약 생성 중 오류 발생: {}", e.getMessage(), e);
            return landmarkName + " 주변 분위기를 분석하는 중 오류가 발생했습니다.";
        }
    }

    private String getSystemPrompt(String reasoningEffort, String verbosity) {
        switch (reasoningEffort) {
            case "minimal":
                return "대학 캠퍼스 커뮤니티 분위기 분석 AI입니다. 빠르고 간결하게 분석합니다.";
            
            case "high":
                return "당신은 대학 캠퍼스 커뮤니티의 전문 분석가입니다. " +
                       "학생들의 게시글을 깊이 있게 분석하여 해당 장소의 현재 분위기, 감정적 동향, " +
                       "주요 이슈들을 종합적으로 파악하고 상세히 설명합니다.";
            
            case "medium":
            default:
                return "당신은 대학 캠퍼스 커뮤니티의 분위기를 분석하는 전문 AI입니다. " +
                       "학생들의 게시글을 바탕으로 해당 장소의 현재 분위기를 친근하고 유익하게 요약해주세요.";
        }
    }

    private String getUserPrompt(String landmarkName, String postsContent, int radius, String verbosity) {
        String lengthInstruction = switch (verbosity) {
            case "low" -> "2-3줄로 간단히";
            case "high" -> "5-6줄로 자세히";
            default -> "3-4줄로";
        };

        return String.format(
            "다음은 %s 주변 %dm 이내의 최근 게시글들입니다.\n" +
            "캠퍼스 학생들이 이 장소 주변에서 어떤 분위기인지 친근하고 유익한 톤으로 %s 요약해주세요.\n\n" +
            
            "주의사항:\n" +
            "- 광고성, 스팸성, 부적절한 내용은 제외하고 요약해 주세요\n" +
            "- 학생들의 실제 캠퍼스 생활과 관련된 내용만 반영해 주세요\n" +
            "- 감정이나 키워드를 중심으로 현재 분위기를 전달해 주세요\n" +
            "- **중요한 키워드**는 굵게 표시해 주세요\n\n" +
            
            "게시글 내용:\n%s",
            landmarkName, radius, lengthInstruction, postsContent
        );
    }

    private List<LandmarkSummaryService.PostData> filterPostsForQuality(List<LandmarkSummaryService.PostData> posts) {
        return posts.stream()
                .filter(this::isValidPost)
                .toList();
    }

    private boolean isValidPost(LandmarkSummaryService.PostData post) {
        String content = post.getTitle() + " " + post.getContents();
        
        // 1. 게시글 길이 체크 (10자 미만 제외)
        if (content.length() < 10) {
            return false;
        }
        
        // 2. 특수문자/이모지 비율 체크 (50% 이상 제외)
        long specialCharCount = content.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();
        
        double specialCharRatio = (double) specialCharCount / content.length();
        return specialCharRatio < 0.5;
    }

    public void clearAdvancedCache(Long landmarkId) {
        // 모든 파라미터 조합의 캐시 삭제
        String[] reasoningLevels = {"minimal", "medium", "high"};
        String[] verbosityLevels = {"low", "medium", "high"};
        
        for (String reasoning : reasoningLevels) {
            for (String verbosity : verbosityLevels) {
                String cacheKey = String.format("%s%d:%s:%s", REDIS_KEY_PREFIX, landmarkId, reasoning, verbosity);
                redisTemplate.delete(cacheKey);
            }
        }
        
        log.info("랜드마크 {} 고급 요약 캐시 모두 삭제", landmarkId);
    }
}