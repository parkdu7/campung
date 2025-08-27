# 캠퍼스 온도 동적 관리 시스템 가이드

## 개요

캠퍼스 온도 동적 관리 시스템은 **감정 분석 결과**와 **게시글 활동도**를 기반으로 실시간 캠퍼스 온도를 자동 조정하는 시스템입니다. 실제 날씨처럼 자연스러운 일일 온도 사이클을 구현하여 캠퍼스 커뮤니티의 활력도를 직관적으로 표현합니다.

## 주요 특징

### 🌡️ 강화된 양방향 온도 조정
- **극도로 활발한 활동**: 온도 상승 (최대 35%) 🚀
- **매우 활발한 활동**: 온도 상승 (최대 25%)
- **일반적 활발함**: 온도 상승 (최대 15%)
- **조용한 시간대**: 온도 하락 (최대 18%)
- **보통 활동**: 온도 유지

### 🛡️ 온도 보호 시스템 (0-100도 범위)
- **극고온 보호**: 95도 이상 시 상승률 95% 억제
- **고온 보호**: 80도 이상 시 상승률 60% 억제  
- **극저온 보호**: 2도 이하 시 하락률 95% 억제
- **저온 보호**: 10도 이하 시 하락률 60% 억제
- **아침 최저 온도 보장**: 매일 오전 6시 5-20도 보장

### ⏰ 24시간 자연 사이클 (확장된 0-100도 범위)
- **새벽(0-5시)**: 0-25도 범위, 점진적 하락
- **아침(6-8시)**: 5-35도 범위, 활동 시작
- **오전(9-12시)**: 10-60도 범위, 활발한 상승
- **오후(13-17시)**: 15-100도 범위, 최대 활성화 🔥
- **저녁(18-20시)**: 10-80도 범위, 높은 온도 유지
- **밤(21-23시)**: 5-50도 범위, 점진적 하락

## 시스템 구조

### 핵심 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│                   매시간 스케줄러                              │
├─────────────────────────────────────────────────────────────┤
│  1. CampusEmotionService.analyzeHourlyEmotions()           │
│     └─> 감정 분석 → 기본 온도 설정                            │
│  2. CampusTemperatureManager.adjustHourlyTemperature()     │
│     ├─> PostActivityAnalyzer (게시글 수 → 활동도)            │
│     ├─> TemperatureGuidelineConfig (시간대 → 가이드라인)      │
│     └─> 양방향 온도 조정 + 보호 로직                         │
└─────────────────────────────────────────────────────────────┘
```

### 데이터베이스 구조

#### DailyCampus 엔티티
```sql
CREATE TABLE daily_campus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL UNIQUE,
    final_temperature DOUBLE NOT NULL,
    weather_type VARCHAR(50) NOT NULL,
    total_post_count INT NOT NULL,
    average_hourly_post_count DOUBLE NOT NULL,
    max_temperature DOUBLE NOT NULL,
    min_temperature DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### CampusTemperature 엔티티
```sql
CREATE TABLE campus_temperature (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp TIMESTAMP NOT NULL,
    current_temperature DOUBLE NOT NULL,
    base_emotion_temperature DOUBLE NOT NULL,
    post_count_adjustment DOUBLE NOT NULL,
    current_hour_post_count INT NOT NULL,
    expected_hourly_average DOUBLE NOT NULL,
    adjustment_reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 온도 조정 로직

### 확장된 활동도 기반 조정률

| 활동 레벨 | 게시글 비율 | 상승률 | 하락률 | 설명 |
|-----------|-------------|--------|--------|------|
| EXTREMELY_HIGH | 300% 이상 | +35% 🔥 | -8%  | 극도로 활발 |
| VERY_HIGH | 200-299%    | +25%   | -10%  | 매우 활발 |
| HIGH      | 150-199%    | +15%   | -8%   | 활발 |
| MODERATE_HIGH | 120-149% | +8%    | -5%   | 약간 활발 |
| NORMAL    | 80-119%     | 0%     | 0%    | 보통 |
| MODERATE_LOW | 60-79%   | -3%    | +5%   | 약간 저조 |
| LOW       | 40-59%      | -8%    | +8%   | 저조 |
| VERY_LOW  | 20-39%      | -12%   | +12%  | 매우 저조 |
| EXTREMELY_LOW | 20% 미만 | -15%   | +18% ❄️ | 극도로 저조 |

### 최종 온도 계산 공식

```
최종온도 = 현재온도 × (1 + 조정률)

조정률 = 기본조정률 × 온도보호계수 × 시간대계수

예시:
- 현재 45도, 게시글 350% (EXTREMELY_HIGH), 오후 3시
- 기본조정률: +35%
- 온도보호계수: 1.0 (45도는 정상 범위)
- 시간대계수: 2.5 (오후 최대 활성화 시간)
- 최종조정률: 0.35 × 1.0 × 2.5 = 87.5%
- 최종온도: 45 × (1 + 0.875) = 84.4도
- 가이드라인 적용: min(84.4, 100.0) = 84.4도 🔥
```

## 스케줄러 동작

### 매시간 정각 (0 0 * * * *)
1. 감정 분석 실행
2. 게시글 활동도 분석
3. 양방향 온도 조정
4. 가이드라인 범위 보정
5. DB 및 Redis 저장

### 매시간 30분 (0 30 * * * *)
- 자연적 온도 회복 (확장된 범위)
- 활동 시간대 온도 상승 (1.0-8.0도)

### 매일 밤 10시 (0 0 22 * * *)
- 다음날 아침 온도 예측
- 10도 미만 예상 시 사전 보정

### 매일 오전 6시 (0 0 6 * * *)
- 아침 최저 온도 보장 (10-20도)
- 온도 보호 모드 해제

### 매일 새벽 5시 (0 0 5 * * *)
- 일일 캠퍼스 데이터 저장
- 감정 데이터 초기화
- 30일 이상 온도 기록 정리

## API 사용법

### 현재 캠퍼스 온도 조회
```java
@Autowired
private CampusTemperatureManager temperatureManager;

double currentTemp = temperatureManager.getCurrentCampusTemperature();
```

### 감정 기반 온도 설정
```java
temperatureManager.setBaseEmotionTemperature(22.5);
```

### 수동 온도 조정
```java
temperatureManager.adjustHourlyTemperature();
```

## 설정 및 커스터마이징

### 시간대별 온도 가이드라인 수정
`TemperatureGuidelineConfig.java`에서 시간대별 설정 변경:

```java
// 새벽 시간대 설정 예시
guidelines.put(hour, new TemperatureGuideline(
    8.0,    // 최저 온도
    15.0,   // 최고 온도  
    0.5,    // 상승률 배수
    1.5     // 하락률 배수
));
```

### 활동도 임계값 조정
`PostActivityAnalyzer.java`에서 활동 레벨 기준 수정:

```java
public enum PostActivityLevel {
    VERY_HIGH(0.15, -0.12, "매우 활발"),  // 상승률, 하락률, 설명
    // ... 다른 레벨들
}
```

### Redis 캐시 TTL 설정
```java
private static final String CURRENT_TEMP_KEY = "campus:temperature:current";
redisTemplate.opsForValue().set(CURRENT_TEMP_KEY, newTemp, Duration.ofMinutes(30));
```

## 모니터링 및 디버깅

### 로그 확인
```bash
# 온도 조정 로그
grep "온도 조정" logs/application.log

# 감정 분석 연동 로그  
grep "감정 분석 결과를 온도 매니저에 전달" logs/application.log

# 스케줄러 실행 로그
grep "매시간 통합 작업" logs/application.log
```

### Redis 데이터 확인
```bash
# 현재 온도
redis-cli GET "campus:temperature:current"

# 감정 기반 온도
redis-cli GET "campus:temperature:base_emotion"

# 게시글 활동 캐시
redis-cli KEYS "post:activity:*"
```

### 데이터베이스 쿼리
```sql
-- 최근 온도 변화 추이
SELECT timestamp, current_temperature, adjustment_reason 
FROM campus_temperature 
WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY timestamp DESC;

-- 일별 캠퍼스 통계
SELECT date, total_post_count, average_hourly_post_count, 
       min_temperature, max_temperature
FROM daily_campus 
ORDER BY date DESC 
LIMIT 7;
```

## 트러블슈팅

### 온도가 계속 떨어지는 경우
1. 게시글 활동도 확인
2. 시간대별 가이드라인 점검
3. 온도 보호 시스템 동작 확인

### 온도 조정이 안 되는 경우
1. 스케줄러 실행 로그 확인
2. Redis 연결 상태 점검
3. 감정 분석 서비스 연동 확인

### 성능 최적화
1. Redis 캐시 TTL 조정
2. 오래된 온도 기록 정리 주기 변경
3. 데이터베이스 인덱스 최적화

## 향후 확장 계획

### 날씨 연동
- 실제 날씨 API와 연동
- 기상 조건에 따른 온도 보정

### 사용자 피드백 반영
- 온도 만족도 조사
- 사용자 맞춤 온도 설정

### 지역별 온도 차별화
- 건물별 온도 관리
- 위치 기반 온도 조정

---

**📝 문서 작성일**: 2025년 8월 27일  
**🔄 마지막 업데이트**: 2025년 8월 27일  
**📧 문의**: 개발팀