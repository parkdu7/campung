package com.example.campung.lankmark.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 공식 GPT-5 가이드를 완전히 준수하는 서비스
 * 모델별 비용 최적화 포함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GPT5ServiceV3 {

    private final GPT5FallbackService gpt5FallbackService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String REDIS_KEY_PREFIX = "landmark:gpt5:";
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * 공식 가이드에 따른 GPT-5 모델별 요약 생성
     * 
     * 모델 비용 (공식 가격):
     * - gpt-5: $1.25/1M input, $10/1M output (최고 품질)
     * - gpt-5-mini: $0.25/1M input, $2/1M output (균형)
     * - gpt-5-nano: $0.05/1M input, $0.40/1M output (최저 비용)
     */
    public String generateOptimizedSummary(Long landmarkId, String landmarkName, 
                                         List<LandmarkSummaryService.PostData> posts, 
                                         int radius, String model, String reasoningEffort, String verbosity) {
        
        // 캐시 무시하고 항상 새로 생성
        String cacheKey = String.format("%s%d:%s:%s", REDIS_KEY_PREFIX, landmarkId, model, verbosity);
        log.info("랜드마크 {} 요약 새로 생성 (캐시 무시, 모델: {}, verbosity: {})", 
                landmarkId, model, verbosity);

        // 데이터 품질 필터링
        List<LandmarkSummaryService.PostData> filteredPosts = filterPostsForQuality(posts);
        
        if (filteredPosts.isEmpty()) {
            return landmarkName + " 주변에 분석할 게시글이 없습니다.";
        }

        // gpt-5만 사용, 다른 모델 제거
        String optimizedModel = "gpt-5";
        
        // GPT-5 계열 모델로 요약 생성
        String summary = generateSummaryWithOptimizedModel(
            optimizedModel, landmarkName, filteredPosts, radius, reasoningEffort, verbosity);
        
        // 성공적인 요약만 캐싱
        if (summary != null && !summary.contains("오류가 발생했습니다")) {
            redisTemplate.opsForValue().set(cacheKey, summary, Duration.ofMinutes(CACHE_TTL_MINUTES));
            log.info("랜드마크 {} 요약 생성 및 캐싱 완료 (모델: {}, 비용 등급: {})", 
                    landmarkId, optimizedModel, getCostGrade(optimizedModel));
        }
        
        return summary;
    }

    /**
     * GPT-5 모델만 사용 (고정)
     */
    private boolean isValidGPT5Model(String model) {
        return "gpt-5".equals(model);
    }

    private String generateSummaryWithOptimizedModel(String model, String landmarkName, 
                                                   List<LandmarkSummaryService.PostData> posts, 
                                                   int radius, String reasoningEffort, String verbosity) {
        try {
            // 게시글 포맷팅
            StringBuilder postsText = new StringBuilder();
            for (LandmarkSummaryService.PostData post : posts) {
                postsText.append("제목: ").append(post.getTitle()).append("\n");
                postsText.append("내용: ").append(post.getContents()).append("\n\n");
            }

            log.info("GPT-5 {} 모델로 요약 생성 시작 ({}개 게시글, verbosity: {}, 예상 비용: {})", 
                    model, posts.size(), verbosity, getEstimatedCost(model, postsText.length()));

            // 공식 Chat Completions API 호출
            String summary = gpt5FallbackService.generateSummaryWithModel(
                model, landmarkName, postsText.toString(), radius, verbosity, reasoningEffort);
            
            if (summary != null && !summary.trim().isEmpty()) {
                log.info("GPT-5 {} 요약 생성 성공 ({}자 → {}자)", 
                        model, postsText.length(), summary.length());
                return summary.trim();
            } else {
                log.error("GPT-5 {}에서 빈 응답 반환", model);
                return landmarkName + " 주변 분위기를 분석할 수 없습니다.";
            }
            
        } catch (Exception e) {
            log.error("GPT-5 {} 요약 생성 중 오류 발생: {}", model, e.getMessage(), e);
            return landmarkName + " 주변 분위기를 분석하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * GPT-5 비용 등급 (고정)
     */
    private String getCostGrade(String model) {
        return "프리미엄 (GPT-5)";
    }

    /**
     * GPT-5 예상 비용 계산
     */
    private String getEstimatedCost(String model, int inputLength) {
        // 대략적인 토큰 계산 (1토큰 ≈ 4자)
        int estimatedTokens = inputLength / 4 + 500; // input + output
        
        // GPT-5: $1.25 input + $10 output (평균 $5.625)
        double costPer1M = 5.625;
        
        double estimatedCost = (estimatedTokens * costPer1M) / 1_000_000;
        return String.format("$%.6f", estimatedCost);
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

    /**
     * 모든 모델의 캐시 삭제
     */
    public void clearAllModelCache(Long landmarkId) {
        String[] models = {"gpt-5", "gpt-5-mini", "gpt-5-nano"};
        String[] verbosityLevels = {"low", "medium", "high"};
        
        for (String model : models) {
            for (String verbosity : verbosityLevels) {
                String cacheKey = String.format("%s%d:%s:%s", REDIS_KEY_PREFIX, landmarkId, model, verbosity);
                redisTemplate.delete(cacheKey);
            }
        }
        
        log.info("랜드마크 {} 모든 GPT-5 모델 캐시 삭제 완료", landmarkId);
    }

    /**
     * 빠른 요약 (gpt-5 + low reasoning + low verbosity)
     */
    public String generateQuickSummary(Long landmarkId, String landmarkName, 
                                     List<LandmarkSummaryService.PostData> posts, int radius) {
        return generateOptimizedSummary(landmarkId, landmarkName, posts, radius, "gpt-5", "low", "low");
    }

    /**
     * 프리미엄 요약 (gpt-5 + high reasoning + high verbosity)
     */
    public String generatePremiumSummary(Long landmarkId, String landmarkName, 
                                       List<LandmarkSummaryService.PostData> posts, int radius) {
        return generateOptimizedSummary(landmarkId, landmarkName, posts, radius, "gpt-5", "high", "high");
    }
}