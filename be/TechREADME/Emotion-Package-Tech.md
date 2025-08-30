# Emotion 패키지 기술 문서

## 개요
Emotion 패키지는 캠퍼스 게시글의 감정을 AI로 분석하여 실시간 캠퍼스 온도를 계산하고, 게시글 활동도를 함께 반영하여 동적으로 관리하는 고도화된 시스템입니다.

## 주요 기술적 특징

### 1. GPT-5 기반 배치 감정 분석 시스템 ⭐

**기술 스택**: OpenAI GPT-5, Batch Processing, Orchestration Pattern

**핵심 구현**:
```java
// PostBatchProcessor.java:24-34
public Map<String, Integer> processBatchEmotions(List<PostData> posts, BatchEmotionAnalyzer analyzer) {
    if (posts.size() <= MAX_BATCH_SIZE) {
        return analyzer.analyzeSingleBatch(posts);
    }
    return processInChunks(posts, analyzer); // 30개씩 청크 분할
}

// EmotionAnalysisService.java:28-35
public Map<String, Integer> analyzeBatchEmotions(List<PostData> posts) {
    Map<String, Integer> result = batchProcessor.processBatchEmotions(posts, this);
    return result; // 6가지 감정: 우울함, 밝음, 신남, 화남, 슬픔, 흥분된
}
```

**기술적 특징**:
- **배치 분할 처리**: 30개씩 청크로 나누어 API 호출 최적화
- **오케스트레이션 패턴**: 다양한 전문 서비스들을 조합하여 복잡한 감정 분석 프로세스 관리
- **Strategy Pattern**: BatchEmotionAnalyzer 인터페이스를 통한 분석 로직 추상화

### 2. 동적 캠퍼스 온도 관리 시스템 ⭐

**핵심 알고리즘**:
```java
// CampusTemperatureManager.java:71-95
private double calculateBidirectionalAdjustment(double currentTemp, int postCount, int hour) {
    // 1. 게시글 활동도 분석
    PostActivityLevel activityLevel = postActivityAnalyzer.analyzeCurrentActivity(postCount);
    
    // 2. 온도 보호 계수 적용 (극한 온도 방지)
    double protectionFactor = getTemperatureProtectionFactor(currentTemp, activityLevel.isIncreasing());
    
    // 3. 시간대별 활동 패턴 반영
    double timeMultiplier = guideline.getIncreaseMultiplier() : guideline.getDecreaseMultiplier();
    
    // 4. 최종 조정률 계산
    double finalRate = baseAdjustmentRate * protectionFactor * timeMultiplier;
}
```

**고급 보호 메커니즘**:
```java
// CampusTemperatureManager.java:100-118
private double getTemperatureProtectionFactor(double currentTemp, boolean isIncreasing) {
    if (isIncreasing) {
        // 95도 이상: 95% 억제, 90도 이상: 80% 억제
        if (currentTemp >= 95.0) return 0.05;
        if (currentTemp >= 90.0) return 0.2;
    } else {
        // 2도 이하: 95% 억제, 5도 이하: 80% 억제
        if (currentTemp <= 2.0) return 0.05;
        if (currentTemp <= 5.0) return 0.2;
    }
}
```

### 3. 고급 스케줄러 시스템

**통합 스케줄링 전략**:
```java
// EmotionAnalysisScheduler.java:27-38
@Scheduled(cron = "0 0 * * * *") // 매시간 정각
public void executeHourlyTasks() {
    // 1. 감정 분석 (감정 기반 온도 계산)
    campusEmotionService.analyzeHourlyEmotions();
    
    // 2. 게시글 활동 기반 온도 조정
    temperatureManager.adjustHourlyTemperature();
}

@Scheduled(cron = "0 30 * * * *") // 매시간 30분
public void naturalTemperatureStabilization() {
    temperatureManager.naturalTemperatureRecovery(); // 자연적 온도 회복
}
```

**예측 및 보정 시스템**:
```java
// EmotionAnalysisScheduler.java:57-64
@Scheduled(cron = "0 0 22 * * *") // 매일 밤 10시
public void preventiveTemperatureAdjustment() {
    temperatureManager.predictAndAdjustForMorning(); // 다음날 아침 온도 예측
}

@Scheduled(cron = "0 0 6 * * *") // 매일 오전 6시
public void ensureMorningTemperature() {
    temperatureManager.ensureMinimumMorningTemperature(); // 최저 온도 보장
}
```

### 4. 실시간 온도 예측 알고리즘

**예측 모델**:
```java
// CampusTemperatureManager.java:298-327
private double predictMorningTemperature() {
    // 현재 시간부터 다음날 6시까지의 예상 변화 계산
    for (int h = currentHour + 1; h <= 24 + 6; h++) {
        int expectedPosts = postActivityAnalyzer.getExpectedPostsForHour(hour);
        
        if (expectedPosts <= expectedAverage * 0.5) {
            // 활동도 감소 시 온도 하락 예측
            double reductionRate = level.getAdjustmentRate() * guideline.getDecreaseMultiplier();
            predictedTemp *= (1 + reductionRate);
        }
        
        // 자연 회복 반영 (7-22시)
        if (hour >= 7 && hour <= 22) {
            predictedTemp += guidelineConfig.getNaturalRecoveryRate(hour) * 0.3;
        }
    }
}
```

### 5. Redis 기반 실시간 캐싱 전략

**다층 캐시 구조**:
```java
// CampusTemperatureManager.java:37-42
private static final String CURRENT_TEMP_KEY = "campus:temperature:current";
private static final String BASE_EMOTION_TEMP_KEY = "campus:temperature:base_emotion";
private static final String TODAY_MAX_TEMP_KEY = "campus:temperature:today_max";
private static final String TODAY_MIN_TEMP_KEY = "campus:temperature:today_min";

// 실시간 최고/최저 온도 업데이트
private void updateTodayMinMaxTemperature(double newTemp) {
    // 현재 최고/최저 온도와 비교하여 실시간 갱신
}
```

### 6. 데이터 영속화 및 통계

**일일 통계 저장**:
```java
// CampusTemperatureManager.java:352-394
public void saveDailyCampusData() {
    // 어제 온도 통계 계산
    Object[] minMaxTemp = temperatureRepository.findMaxMinTemperatureByDate(startTime);
    
    // 어제 게시글 통계
    int totalPosts = contentRepository.countByCreatedAtBetween(startTime, endTime);
    
    // DailyCampus 저장
    DailyCampus dailyCampus = DailyCampus.builder()
            .date(yesterday)
            .finalTemperature(finalTemp)
            .totalPostCount(totalPosts)
            .maxTemperature(maxTemp)
            .minTemperature(minTemp)
            .build();
}
```

## 성능 최적화 포인트

1. **배치 처리 최적화**: 30개 단위 청크로 API 호출 효율화
2. **Redis 캐싱**: 실시간 온도 데이터 30분 TTL 캐싱
3. **스케줄링 분산**: 다양한 시간대별 작업 분산으로 서버 부하 최소화
4. **예측 알고리즘**: 사전 온도 조정으로 극단적 온도 변화 방지

## 확장성 고려사항

- **다중 캠퍼스 지원**: 캠퍼스별 독립적인 온도 관리 시스템 확장 가능
- **머신러닝 통합**: 과거 데이터를 활용한 예측 모델 고도화
- **실시간 알림**: WebSocket을 통한 온도 변화 실시간 알림
- **외부 API 연동**: 실제 날씨 데이터와의 융합 분석

이 시스템을 통해 단순한 감정 분석을 넘어 **실시간 캠퍼스 분위기의 온도화**와 **예측 기반 동적 관리**를 구현했습니다.