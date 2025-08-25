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
public class LandmarkSummaryService {

    private final GPT5ResponsesService gpt5ResponsesService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String REDIS_KEY_PREFIX = "landmark:summary:";
    private static final long CACHE_TTL_MINUTES = 30;

    public String generateSummary(Long landmarkId, String landmarkName, List<PostData> posts, int radius) {
        // Redis 캐시에서 먼저 확인
        String cacheKey = REDIS_KEY_PREFIX + landmarkId;
        String cachedSummary = (String) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedSummary != null) {
            log.info("랜드마크 {} 요약 캐시에서 반환", landmarkId);
            return cachedSummary;
        }

        // 데이터 품질 필터링
        List<PostData> filteredPosts = filterPostsForQuality(posts);
        
        if (filteredPosts.isEmpty()) {
            return landmarkName + " 주변에 분석할 게시글이 없습니다.";
        }

        // GPT-5 API 호출하여 요약 생성
        String summary = generateSummaryWithGPT5(landmarkName, filteredPosts, radius);
        
        // Redis에 캐싱 (30분 TTL)
        redisTemplate.opsForValue().set(cacheKey, summary, Duration.ofMinutes(CACHE_TTL_MINUTES));
        
        log.info("랜드마크 {} 요약 생성 완료 및 캐싱", landmarkId);
        return summary;
    }

    private List<PostData> filterPostsForQuality(List<PostData> posts) {
        return posts.stream()
                .filter(this::isValidPost)
                .toList();
    }

    private boolean isValidPost(PostData post) {
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

    private String generateSummaryWithGPT5(String landmarkName, List<PostData> posts, int radius) {
        try {
            // 게시글 목록을 텍스트로 포맷팅
            StringBuilder postsText = new StringBuilder();
            for (PostData post : posts) {
                postsText.append("제목: ").append(post.getTitle()).append("\n");
                postsText.append("내용: ").append(post.getContents()).append("\n\n");
            }

            log.info("GPT-5로 랜드마크 {} 요약 생성 시작 ({}개 게시글)", landmarkName, posts.size());

            // GPT-5 Responses API 호출
            String summary = gpt5ResponsesService.generateCampusSummary(
                landmarkName, 
                postsText.toString(), 
                radius
            );
            
            if (summary != null && !summary.trim().isEmpty()) {
                log.info("GPT-5 요약 생성 성공: {}", summary.substring(0, Math.min(50, summary.length())) + "...");
                return summary.trim();
            } else {
                log.error("GPT-5에서 빈 응답 반환");
                return landmarkName + " 주변 분위기를 분석할 수 없습니다.";
            }
            
        } catch (Exception e) {
            log.error("GPT-5 요약 생성 중 오류 발생: {}", e.getMessage(), e);
            return landmarkName + " 주변 분위기를 분석하는 중 오류가 발생했습니다.";
        }
    }

    public void clearCache(Long landmarkId) {
        String cacheKey = REDIS_KEY_PREFIX + landmarkId;
        redisTemplate.delete(cacheKey);
        log.info("랜드마크 {} 요약 캐시 삭제", landmarkId);
    }

    // 내부 클래스: 게시글 데이터
    public static class PostData {
        private String title;
        private String contents;

        public PostData(String title, String contents) {
            this.title = title;
            this.contents = contents;
        }

        public String getTitle() { return title; }
        public String getContents() { return contents; }
    }
}