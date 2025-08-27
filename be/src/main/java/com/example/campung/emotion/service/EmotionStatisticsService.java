package com.example.campung.emotion.service;

import com.example.campung.global.enums.EmotionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 감정 분석 통계 데이터 관리 서비스
 * Redis 데이터 저장/조회 전용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionStatisticsService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_SCORE_KEY = "emotion:daily:scores:";
    private static final String REDIS_COUNT_KEY = "emotion:daily:count:";
    private static final String REDIS_WEATHER_KEY = "emotion:daily:weather";
    private static final String REDIS_TEMPERATURE_KEY = "emotion:daily:temperature";
    private static final String REDIS_TOTAL_POSTS_KEY = "emotion:daily:total_posts";
    private static final String REDIS_CUMULATIVE_SCORES_KEY = "emotion:daily:cumulative_scores:";
    private static final String REDIS_HOURLY_POSTS_KEY = "emotion:daily:hourly_posts:";

    /**
     * 일일 평균 감정 점수 업데이트
     */
    public void updateDailyAverageScores(Map<String, Integer> newScores) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        for (EmotionType emotionType : EmotionType.values()) {
            String emotionKey = emotionType.getKoreanName();
            String scoreKey = REDIS_SCORE_KEY + today + ":" + emotionKey;
            String countKey = REDIS_COUNT_KEY + today + ":" + emotionKey;
            
            Integer newScore = newScores.get(emotionKey);
            if (newScore == null) newScore = 50;
            
            Object currentScoreObj = redisTemplate.opsForValue().get(scoreKey);
            Object currentCountObj = redisTemplate.opsForValue().get(countKey);
            
            double currentScore = currentScoreObj != null ? Double.parseDouble(currentScoreObj.toString()) : 0.0;
            int currentCount = currentCountObj != null ? Integer.parseInt(currentCountObj.toString()) : 0;
            
            double newAverage = (currentScore * currentCount + newScore) / (currentCount + 1);
            
            redisTemplate.opsForValue().set(scoreKey, newAverage);
            redisTemplate.opsForValue().set(countKey, currentCount + 1);
        }
        
        log.info("일일 평균 감정 점수 업데이트 완료: {}", today);
    }

    /**
     * 총 분석된 게시글 수 업데이트
     */
    public void updateTotalPostsCount(int newPostCount) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String totalPostsKey = REDIS_TOTAL_POSTS_KEY + ":" + today;
        
        Object currentCountObj = redisTemplate.opsForValue().get(totalPostsKey);
        int currentCount = currentCountObj != null ? Integer.parseInt(currentCountObj.toString()) : 0;
        
        redisTemplate.opsForValue().set(totalPostsKey, currentCount + newPostCount);
    }

    /**
     * 누적 감정 점수 업데이트
     */
    public void updateCumulativeScores(Map<String, Integer> newScores) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        for (EmotionType emotionType : EmotionType.values()) {
            String emotionKey = emotionType.getKoreanName();
            String cumulativeKey = REDIS_CUMULATIVE_SCORES_KEY + today + ":" + emotionKey;
            
            Integer newScore = newScores.get(emotionKey);
            if (newScore == null) newScore = 50;
            
            Object currentCumulativeObj = redisTemplate.opsForValue().get(cumulativeKey);
            int currentCumulative = currentCumulativeObj != null ? Integer.parseInt(currentCumulativeObj.toString()) : 0;
            
            redisTemplate.opsForValue().set(cumulativeKey, currentCumulative + newScore);
        }
    }

    /**
     * 시간대별 분석된 게시글 수 업데이트
     */
    public void updateHourlyPostsCount(LocalDateTime analysisTime, int postCount) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int hour = analysisTime.getHour();
        String hourlyKey = REDIS_HOURLY_POSTS_KEY + today + ":" + hour;
        
        Object currentHourlyCountObj = redisTemplate.opsForValue().get(hourlyKey);
        int currentHourlyCount = currentHourlyCountObj != null ? Integer.parseInt(currentHourlyCountObj.toString()) : 0;
        
        redisTemplate.opsForValue().set(hourlyKey, currentHourlyCount + postCount);
    }

    /**
     * 감정 날씨와 온도 저장
     */
    public void updateWeatherAndTemperature(String weather, Double temperature) {
        redisTemplate.opsForValue().set(REDIS_WEATHER_KEY, weather);
        redisTemplate.opsForValue().set(REDIS_TEMPERATURE_KEY, temperature);
        
        log.info("감정 날씨/온도 업데이트: {} / {}°C", weather, temperature);
    }

    /**
     * 현재 일일 평균 감정 점수 조회
     */
    public Map<String, Double> getCurrentDailyAverageScores() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Double> averageScores = new HashMap<>();
        
        for (EmotionType emotionType : EmotionType.values()) {
            String emotionKey = emotionType.getKoreanName();
            String scoreKey = REDIS_SCORE_KEY + today + ":" + emotionKey;
            
            Object scoreObj = redisTemplate.opsForValue().get(scoreKey);
            double score = scoreObj != null ? Double.parseDouble(scoreObj.toString()) : 50.0;
            
            averageScores.put(emotionKey, score);
        }
        
        return averageScores;
    }

    /**
     * 현재 감정 날씨 조회
     */
    public String getCurrentEmotionWeather() {
        Object weather = redisTemplate.opsForValue().get(REDIS_WEATHER_KEY);
        return weather != null ? weather.toString() : "흐림";
    }

    /**
     * 현재 감정 온도 조회
     */
    public Double getCurrentEmotionTemperature() {
        Object temperature = redisTemplate.opsForValue().get(REDIS_TEMPERATURE_KEY);
        return temperature != null ? Double.parseDouble(temperature.toString()) : 20.0;
    }

    /**
     * 총 분석된 게시글 수 조회
     */
    public int getTotalAnalyzedPostsCount() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String totalPostsKey = REDIS_TOTAL_POSTS_KEY + ":" + today;
        
        Object countObj = redisTemplate.opsForValue().get(totalPostsKey);
        return countObj != null ? Integer.parseInt(countObj.toString()) : 0;
    }

    /**
     * 누적 감정 점수 조회
     */
    public Map<String, Integer> getCumulativeEmotionScores() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Integer> cumulativeScores = new HashMap<>();
        
        for (EmotionType emotionType : EmotionType.values()) {
            String emotionKey = emotionType.getKoreanName();
            String cumulativeKey = REDIS_CUMULATIVE_SCORES_KEY + today + ":" + emotionKey;
            
            Object scoreObj = redisTemplate.opsForValue().get(cumulativeKey);
            int score = scoreObj != null ? Integer.parseInt(scoreObj.toString()) : 0;
            
            cumulativeScores.put(emotionKey, score);
        }
        
        return cumulativeScores;
    }

    /**
     * 시간대별 분석된 게시글 수 조회
     */
    public Map<String, Integer> getHourlyAnalyzedPostsCount() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Integer> hourlyPosts = new HashMap<>();
        
        for (int hour = 0; hour < 24; hour++) {
            String hourlyKey = REDIS_HOURLY_POSTS_KEY + today + ":" + hour;
            
            Object countObj = redisTemplate.opsForValue().get(hourlyKey);
            int count = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
            
            hourlyPosts.put(String.format("%02d:00", hour), count);
        }
        
        return hourlyPosts;
    }

    /**
     * 매일 새벽 5시 데이터 초기화
     */
    public void resetDailyEmotionData() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        for (EmotionType emotionType : EmotionType.values()) {
            String emotionKey = emotionType.getKoreanName();
            String scoreKey = REDIS_SCORE_KEY + today + ":" + emotionKey;
            String countKey = REDIS_COUNT_KEY + today + ":" + emotionKey;
            String cumulativeKey = REDIS_CUMULATIVE_SCORES_KEY + today + ":" + emotionKey;
            
            redisTemplate.delete(scoreKey);
            redisTemplate.delete(countKey);
            redisTemplate.delete(cumulativeKey);
        }
        
        redisTemplate.delete(REDIS_WEATHER_KEY);
        redisTemplate.delete(REDIS_TEMPERATURE_KEY);
        
        String totalPostsKey = REDIS_TOTAL_POSTS_KEY + ":" + today;
        redisTemplate.delete(totalPostsKey);
        
        for (int hour = 0; hour < 24; hour++) {
            String hourlyKey = REDIS_HOURLY_POSTS_KEY + today + ":" + hour;
            redisTemplate.delete(hourlyKey);
        }
        
        log.info("일일 감정 데이터 초기화 완료: {}", today);
    }
}