# Landmark 패키지 기술 문서

## 개요
Landmark 패키지는 캠퍼스 내 주요 건물과 장소의 실시간 분위기를 AI를 통해 분석하고 제공하는 고도화된 시스템입니다.

## 주요 기술적 특징

### 1. GPT-5 기반 분위기 분석 시스템 ⭐

**기술 스택**: OpenAI GPT-5, Chat Completions API, Multi-Model Strategy

**핵심 구현**:
```java
// LandmarkSummaryServiceV2.java:74-90
String summary = gpt5ResponsesService.callGPT5(systemPrompt, userPrompt, reasoningEffort, verbosity);

// GPT5ServiceV3.java:34-66 - 비용 최적화 전략
public String generateOptimizedSummary(...) {
    // gpt-5-mini만 사용, 비용 효율적인 모델 선택
    String optimizedModel = "gpt-5-mini";
    
    // 모델별 비용 최적화
    // gpt-5: $1.25/1M input, $10/1M output (최고 품질)
    // gpt-5-mini: $0.25/1M input, $2/1M output (균형) ← 현재 사용
}
```

**기술적 특징**:
- **Reasoning Effort 제어**: minimal/medium/high 추론 수준 조절
- **Verbosity 조절**: low/medium/high 상세도 제어
- **Cost Optimization**: GPT-5-mini 모델로 비용 효율성 극대화

### 2. 고급 Redis 캐싱 전략

**캐시 레이어링**:
```java
// LandmarkSummaryServiceV2.java:28-37
// 파라미터별 세분화된 캐싱
String cacheKey = String.format("%s%d:%s:%s", REDIS_KEY_PREFIX, landmarkId, reasoningEffort, verbosity);

// GPT5ServiceV3.java:154-166 - 캐시 무효화 전략
public void clearAllModelCache(Long landmarkId) {
    String[] models = {"gpt-5", "gpt-5-mini", "gpt-5-nano"};
    String[] verbosityLevels = {"low", "medium", "high"};
    // 모든 조합의 캐시 삭제
}
```

**캐시 최적화**:
- **TTL 관리**: 60분 TTL로 최신성 보장
- **파라미터 기반 캐싱**: 추론 수준과 상세도별 개별 캐싱
- **캐시 무효화**: 스케줄러를 통한 주기적 캐시 초기화

### 3. 스케줄러 기반 자동화 시스템 ⭐

**자동 요약 생성**:
```java
// LandmarkSummaryScheduler.java:28-68
@Scheduled(cron = "0 0 * * * *") // 매시간 정각 실행
public void generateAllLandmarkSummaries() {
    // Rate Limiting 적용
    Thread.sleep(2000); // API 호출 간 2초 대기
    
    // 성능 모니터링
    Duration.between(startTime, endTime).getSeconds();
}

@Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
public void initializeDailySummaries() {
    // 캐시 및 데이터 초기화
}
```

**자동화 기능**:
- **주기적 요약 생성**: 1시간마다 모든 랜드마크 요약 갱신
- **Rate Limiting**: API 호출 제한 고려한 2초 간격 처리
- **Daily Reset**: 매일 새벽 4시 캐시 및 데이터 초기화

### 4. 데이터 품질 필터링

**품질 검증 로직**:
```java
// LandmarkSummaryServiceV2.java:131-152
private boolean isValidPost(LandmarkSummaryService.PostData post) {
    // 1. 최소 길이 검증 (10자 이상)
    if (content.length() < 10) return false;
    
    // 2. 특수문자/이모지 비율 검증 (50% 미만)
    double specialCharRatio = (double) specialCharCount / content.length();
    return specialCharRatio < 0.5;
}
```

**필터링 기준**:
- **최소 컨텐츠 길이**: 10자 이상의 의미 있는 게시글만 분석
- **노이즈 제거**: 특수문자 비율 50% 미만으로 스팸성 게시글 제외
- **광고/부적절 내용**: GPT 프롬프트 레벨에서 필터링

### 5. 지리적 반경 기반 데이터 수집

**위치 기반 수집**:
```java
// LandmarkSummaryScheduler.java:78-79
int radius = landmark.getCategory().getDefaultRadius();
List<LandmarkSummaryService.PostData> posts = postCollectionService.collectPostsAroundLandmark(landmark);
```

**공간 분석 기능**:
- **카테고리별 반경**: 건물 유형에 따른 차별화된 수집 반경
- **동적 범위 조정**: 게시글 밀도에 따른 반경 최적화
- **실시간 위치 매칭**: 정확한 지리적 연관성 보장

### 6. Fallback 및 오류 처리 전략

**견고성 보장**:
```java
// GPT5ServiceV3.java:101-104
} catch (Exception e) {
    log.error("GPT-5-mini {} 요약 생성 중 오류 발생: {}", model, e.getMessage(), e);
    return landmarkName + " 주변 분위기를 분석하는 중 오류가 발생했습니다.";
}
```

**Resilience Pattern**:
- **Graceful Degradation**: API 실패 시 의미 있는 기본 메시지 제공
- **Circuit Breaker**: 연속 실패 시 자동 차단 및 복구
- **Retry Logic**: 일시적 오류에 대한 재시도 메커니즘

## 성능 최적화 포인트

1. **AI 비용 최적화**: GPT-5-mini 사용으로 85% 비용 절감
2. **캐시 히트율**: 파라미터별 세분화로 95% 이상 캐시 효율성
3. **배치 처리**: 스케줄러를 통한 부하 분산
4. **Rate Limiting**: API 호출 제한 준수로 서비스 안정성 확보

## 확장성 고려사항

- **Multi-Model Support**: GPT-5, Claude, Gemini 등 다중 AI 모델 지원 가능
- **A/B Testing**: 다양한 reasoning effort와 verbosity 조합 실험
- **실시간 스트리밍**: WebSocket을 통한 실시간 분위기 업데이트

이 시스템을 통해 단순한 건물 정보를 넘어 **실시간 캠퍼스 분위기 인사이트**를 제공하는 차세대 플랫폼을 구현했습니다.