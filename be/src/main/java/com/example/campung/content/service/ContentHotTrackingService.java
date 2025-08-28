package com.example.campung.content.service;

import com.example.campung.content.repository.ContentLikeRepository;
import com.example.campung.entity.ContentLike;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ContentHotTrackingService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ContentLikeRepository contentLikeRepository;
    
    private static final String LIKE_KEY_PREFIX = "content:likes:24h:";
    private static final String HOT_RANKING_KEY = "hot:content:ranking";
    private static final long TWENTY_FOUR_HOURS = 24 * 60 * 60; // 24시간 (초)
    
    public void trackLike(Long contentId, String userId) {
        String key = LIKE_KEY_PREFIX + contentId;
        long timestamp = Instant.now().getEpochSecond();
        
        // Redis Sorted Set에 좋아요 추가 (score = timestamp)
        redisTemplate.opsForZSet().add(key, userId, timestamp);
        
        // 24시간 TTL 설정
        redisTemplate.expire(key, java.time.Duration.ofSeconds(TWENTY_FOUR_HOURS));
        
        // 24시간 이전 데이터 삭제
        cleanupOldLikes(key);
    }
    
    public void removeLike(Long contentId, String userId) {
        String key = LIKE_KEY_PREFIX + contentId;
        redisTemplate.opsForZSet().remove(key, userId);
    }
    
    public long getLike24hCount(Long contentId) {
        String key = LIKE_KEY_PREFIX + contentId;
        cleanupOldLikes(key);
        return redisTemplate.opsForZSet().count(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
    
    public void updateHotRanking(Long contentId, long likeCount) {
        redisTemplate.opsForZSet().add(HOT_RANKING_KEY, contentId, likeCount);
    }
    
    public Set<Object> getTop10HotContent() {
        // 점수 높은 순으로 상위 10개 반환
        return redisTemplate.opsForZSet().reverseRange(HOT_RANKING_KEY, 0, 9);
    }
    
    private void cleanupOldLikes(String key) {
        long twentyFourHoursAgo = Instant.now().getEpochSecond() - TWENTY_FOUR_HOURS;
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, twentyFourHoursAgo);
    }
    
    public void migrateExistingLikesToRedis() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<ContentLike> recentLikes = contentLikeRepository.findAllSince(twentyFourHoursAgo);
        
        for (ContentLike like : recentLikes) {
            String key = LIKE_KEY_PREFIX + like.getContent().getContentId();
            long timestamp = like.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            
            redisTemplate.opsForZSet().add(key, like.getUser().getUserId(), timestamp);
            redisTemplate.expire(key, java.time.Duration.ofSeconds(TWENTY_FOUR_HOURS));
        }
        
        // 모든 컨텐츠의 HOT 랭킹 업데이트
        updateAllHotRankings();
    }
    
    private void updateAllHotRankings() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<ContentLike> recentLikes = contentLikeRepository.findAllSince(twentyFourHoursAgo);
        
        // 컨텐츠별로 그룹화하여 카운트
        recentLikes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        like -> like.getContent().getContentId(),
                        java.util.stream.Collectors.counting()
                ))
                .forEach((contentId, count) -> {
                    updateHotRanking(contentId, count);
                });
    }
}