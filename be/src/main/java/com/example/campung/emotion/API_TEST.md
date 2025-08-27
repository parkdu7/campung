# 캠퍼스 감정 날씨 API 테스트 가이드

## 1. 메인 화면 API (감정 데이터 포함)

### URL
```
GET /api/map
```

### Request Parameters
```
lat=37.5665        # 위도 (필수)
lng=126.9780       # 경도 (필수) 
radius=1000        # 반경 (미터, 필수)
postType=GENERAL   # 게시글 타입 (선택: GENERAL, QUESTION, ANNOUNCEMENT, EVENT)
date=2025-01-27    # 날짜 (선택, 기본값: 오늘)
```

### Full Test URL
```
http://localhost:8080/api/map?lat=37.5665&lng=126.9780&radius=1000&postType=GENERAL&date=2025-01-27
```

### Expected Response
```json
{
  "success": true,
  "message": "지도 콘텐츠 조회 성공",
  "data": {
    "contents": [
      {
        "contentId": 1,
        "userId": "user123",
        "author": {
          "nickname": "홍길동",
          "isAnonymous": false
        },
        "location": {
          "latitude": 37.5665,
          "longitude": 126.9780
        },
        "postType": "GENERAL",
        "postTypeName": "일반",
        "markerType": "GENERAL",
        "contentScope": "MAP",
        "contentType": "TEXT",
        "title": "게시글 제목",
        "body": "게시글 내용",
        "emotionTag": "밝음",
        "reactions": {
          "likes": 5,
          "comments": 3
        },
        "createdAt": "2025-01-27T10:30:00Z",
        "expiresAt": "2025-02-03T10:30:00Z"
      }
    ],
    "records": [],
    "totalCount": 1,
    "hasMore": false,
    "emotionWeather": "맑음",
    "emotionTemperature": 28.5
  }
}
```

## 2. 감정 통계 API (직접 조회)

### URL
```
GET /api/emotion/statistics
```

### Request
```
# GET 요청, 파라미터 없음
```

### Full Test URL
```
http://localhost:8080/api/emotion/statistics
```

### Expected Response
```json
{
  "success": true,
  "data": {
    "averageScores": {
      "우울함": 45.2,
      "밝음": 78.9,
      "신남": 65.3,
      "화남": 32.1,
      "슬픔": 38.7,
      "흥분된": 58.4
    },
    "emotionWeather": "구름 조금",
    "emotionTemperature": 24.3,
    "lastUpdated": "2025-01-27T15:00:00"
  }
}
```

## 3. 수동 감정 분석 API

### URL
```
POST /api/emotion/analyze
```

### Request
```
# POST 요청, Body 없음
```

### Full Test URL
```
http://localhost:8080/api/emotion/analyze
```

### Expected Response
```json
{
  "success": true,
  "message": "감정 분석이 완료되었습니다",
  "data": {
    "averageScores": {
      "우울함": 45.2,
      "밝음": 78.9,
      "신남": 65.3,
      "화남": 32.1,
      "슬픔": 38.7,
      "흥분된": 58.4
    },
    "weather": "구름 조금",
    "temperature": 24.3,
    "lastUpdated": "2025-01-27T15:00:00"
  }
}
```

## 4. 감정 데이터 초기화 API

### URL
```
POST /api/emotion/reset
```

### Request
```
# POST 요청, Body 없음
```

### Full Test URL
```
http://localhost:8080/api/emotion/reset
```

### Expected Response
```json
{
  "success": true,
  "message": "감정 데이터가 초기화되었습니다",
  "resetTime": "2025-01-27T15:00:00"
}
```

## 5. 감정 날씨 간단 조회 API

### URL
```
GET /api/emotion/weather
```

### Request
```
# GET 요청, 파라미터 없음
```

### Full Test URL
```
http://localhost:8080/api/emotion/weather
```

### Expected Response
```json
{
  "success": true,
  "data": {
    "emotionWeather": "맑음",
    "emotionTemperature": 28.5,
    "lastUpdated": "2025-01-27T15:00:00"
  }
}
```

## 6. 감정 점수만 조회 API

### URL
```
GET /api/emotion/scores
```

### Request
```
# GET 요청, 파라미터 없음
```

### Full Test URL
```
http://localhost:8080/api/emotion/scores
```

### Expected Response
```json
{
  "success": true,
  "data": {
    "averageScores": {
      "우울함": 45.2,
      "밝음": 78.9,
      "신남": 65.3,
      "화남": 32.1,
      "슬픔": 38.7,
      "흥분된": 58.4
    },
    "lastUpdated": "2025-01-27T15:00:00"
  }
}
```

## 7. cURL 테스트 명령어

### 메인 화면 API 테스트
```bash
curl -X GET "http://localhost:8080/api/map?lat=37.5665&lng=126.9780&radius=1000" \
  -H "Content-Type: application/json"
```

### 감정 통계 API 테스트
```bash
curl -X GET "http://localhost:8080/api/emotion/statistics" \
  -H "Content-Type: application/json"
```

### 수동 감정 분석 실행
```bash
curl -X POST "http://localhost:8080/api/emotion/analyze" \
  -H "Content-Type: application/json"
```

### 감정 데이터 초기화
```bash
curl -X POST "http://localhost:8080/api/emotion/reset" \
  -H "Content-Type: application/json"
```

### 감정 날씨만 조회
```bash
curl -X GET "http://localhost:8080/api/emotion/weather" \
  -H "Content-Type: application/json"
```

### 감정 점수만 조회
```bash
curl -X GET "http://localhost:8080/api/emotion/scores" \
  -H "Content-Type: application/json"
```

## 4. Postman 테스트 설정

### Collection: Campus Emotion Weather API

#### Request 1: Main Screen with Emotion Data
- **Method**: GET
- **URL**: `{{baseURL}}/api/map`
- **Params**:
  - lat: 37.5665
  - lng: 126.9780
  - radius: 1000
  - postType: GENERAL (optional)
  - date: 2025-01-27 (optional)

#### Request 2: Emotion Statistics
- **Method**: GET  
- **URL**: `{{baseURL}}/api/emotion/statistics`

### Environment Variables
```json
{
  "baseURL": "http://localhost:8080/api"
}
```

## 5. 스케줄러 동작 확인

### 로그 확인 방법
```bash
# 애플리케이션 실행 후 로그 확인
tail -f logs/spring.log | grep "감정"

# 또는 콘솔 로그에서 확인할 키워드
grep -E "(매시간 감정|일일 감정|감정 분석)" application.log
```

### 예상 로그 메시지
```
2025-01-27 14:00:00 - 매시간 감정 분석 스케줄러 시작
2025-01-27 14:00:01 - 시간별 감정 분석 시작: 2025-01-27T13:00 ~ 2025-01-27T14:00
2025-01-27 14:00:02 - GPT-5 감정 분석 API 호출 시작 (프롬프트 길이: 1240자)
2025-01-27 14:00:05 - 감정 분석 완료: {우울함=45, 밝음=78, 신남=65, 화남=32, 슬픔=38, 흥분된=58}
2025-01-27 14:00:05 - 일일 평균 감정 점수 업데이트 완료: 2025-01-27
2025-01-27 14:00:05 - 감정 날씨/온도 업데이트: 구름 조금 / 24.3°C
2025-01-27 14:00:05 - 매시간 감정 분석 스케줄러 완료
```

## 6. Redis 데이터 확인

### Redis CLI 명령어
```bash
# Redis 접속
redis-cli

# 감정 데이터 확인
KEYS emotion:*

# 특정 키 조회
GET emotion:daily:weather
GET emotion:daily:temperature
GET emotion:daily:scores:2025-01-27:밝음
GET emotion:daily:count:2025-01-27:밝음
```

## 7. 테스트 시나리오

### 시나리오 1: 정상 동작 테스트
1. 애플리케이션 시작
2. 테스트 게시글 몇 개 생성 (1시간 이내)
3. 메인 API 호출하여 기본값 확인 (흐림, 20.0°C)
4. 다음 시간 정각 대기 (스케줄러 동작)
5. 다시 메인 API 호출하여 업데이트된 값 확인

### 시나리오 2: 오류 처리 테스트
1. GPT-5 API 키 제거 후 스케줄러 동작 확인
2. Redis 서버 중지 후 API 호출 (GlobalExceptionHandler 동작 확인)
3. 잘못된 파라미터로 API 호출

### 시나리오 3: 성능 테스트
1. 대량 게시글 생성 (30개 초과)
2. 배치 분할 처리 로그 확인
3. API 응답 시간 측정

## 8. 디버깅 도구

### 감정 분석 강제 실행 (개발용)
```java
@RestController
@RequestMapping("/api/debug")
public class EmotionDebugController {
    
    @Autowired
    private CampusEmotionService campusEmotionService;
    
    @PostMapping("/analyze-now")
    public String forceAnalyze() {
        campusEmotionService.analyzeHourlyEmotions();
        return "감정 분석 강제 실행 완료";
    }
    
    @PostMapping("/reset")
    public String resetData() {
        campusEmotionService.resetDailyEmotionData();
        return "감정 데이터 초기화 완료";
    }
}
```

### 디버그 API URL
```
POST http://localhost:8080/api/debug/analyze-now  # 감정 분석 강제 실행
POST http://localhost:8080/api/debug/reset        # 데이터 초기화
```