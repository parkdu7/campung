package com.example.campung.content.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
public class ContentHotTrackingService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
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
}