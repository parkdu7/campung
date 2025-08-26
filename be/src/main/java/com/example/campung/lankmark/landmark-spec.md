# Landmark 기능 명세서

## 개요
대학 캠퍼스 지도 기반 커뮤니티에서 중요한 장소(랜드마크)를 관리하고, 해당 위치의 실시간 분위기를 AI로 요약하는 기능

## 엔티티 설계
```java
@Entity
public class Landmark {
    private Long id;
    private String name;              // 랜드마크 이름
    private String description;       // 랜드마크 설명
    private Double latitude;          // 위도
    private Double longitude;         // 경도
    private LandmarkCategory category; // 카테고리 (도서관, 카페, 강의실 등)
    private String imageUrl;          // 대표 이미지 URL
    private String thumbnailUrl;      // 썸네일 이미지 URL (로딩 최적화용)
    private Long viewCount;           // 조회수
    private Long likeCount;           // 좋아요 수
    private String currentSummary;    // 현재 요약
    private LocalDateTime summaryUpdatedAt; // 요약 업데이트 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## API 명세서

### 1. 랜드마크 등록
- **URL**: `POST /api/landmark`
- **Request Body**:
  ```json
  {
    "name": "중앙도서관",
    "description": "24시간 운영하는 메인 도서관",
    "latitude": 37.123456,
    "longitude": 127.123456,
    "category": "LIBRARY",
    "imageUrl": "https://example.com/image.jpg",
    "thumbnailUrl": "https://example.com/thumbnail.jpg"
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "data": {
      "id": 1,
      "name": "중앙도서관",
      "createdAt": "2025-01-01T10:00:00"
    }
  }
  ```

### 2. 랜드마크 수정
- **URL**: `PUT /api/landmark/{id}`
- **Request Body**: 등록과 동일
- **Response**:
  ```json
  {
    "success": true,
    "data": {
      "id": 1,
      "updatedAt": "2025-01-01T11:00:00"
    }
  }
  ```

### 3. 랜드마크 삭제
- **URL**: `DELETE /api/landmark/{id}`
- **Response**:
  ```json
  {
    "success": true,
    "message": "랜드마크가 삭제되었습니다"
  }
  ```

### 4. 랜드마크 상세 조회
- **URL**: `GET /api/landmark/{id}`
- **Response**:
  ```json
  {
    "success": true,
    "data": {
      "id": 1,
      "name": "중앙도서관",
      "description": "24시간 운영하는 메인 도서관",
      "latitude": 37.123456,
      "longitude": 127.123456,
      "category": "LIBRARY",
      "imageUrl": "https://example.com/image.jpg",
      "thumbnailUrl": "https://example.com/thumbnail.jpg",
      "viewCount": 150,
      "likeCount": 23,
      "currentSummary": "최근 중앙도서관 주변에서는 **'시험 기간'**과 관련된 '불안함' 감정이 가장 많이 나타나고 있어요...",
      "summaryUpdatedAt": "2025-01-01T12:30:00"
    }
  }
  ```

### 5. 랜드마크 요약 생성
- **URL**: `POST /api/landmark/{landmarkId}/summary`
- **Description**: GPT API를 호출하여 실시간으로 해당 랜드마크 주변 게시글들을 분석하고 요약 생성
- **Response**:
  ```json
  {
    "success": true,
    "data": {
      "landmarkId": 1,
      "summary": "최근 중앙도서관 주변에서는...",
      "generatedAt": "2025-01-01T12:30:00",
      "postCount": 15,
      "keywords": ["시험", "스터디", "커피", "자리"]
    }
  }
  ```

### 6. 현재 위치 기반 주변 랜드마크 조회
- **URL**: `GET /api/landmarks/nearby?lat={latitude}&lng={longitude}&radius={meters}`
- **Query Parameters**:
  - `lat`: 위도 (required)
  - `lng`: 경도 (required) 
  - `radius`: 반경(미터, default: 1000)
- **Response**:
  ```json
  {
    "success": true,
    "data": [
      {
        "id": 1,
        "name": "중앙도서관",
        "category": "LIBRARY",
        "distance": 150,
        "thumbnailUrl": "https://example.com/thumbnail.jpg",
        "likeCount": 23,
        "hasActiveSummary": true
      }
    ]
  }
  ```

## 요약 생성 로직

### 스케줄러 설정
- **주기**: 30분마다 실행
- **초기화**: 매일 04:00에 요약 데이터 초기화
- **누적**: 04:00부터 다음날 04:00까지 24시간 누적 요약

### 데이터 수집 범위
- **기본 반경**: 500m
- **카테고리별 적응적 범위**:
  - 도서관: 300m (집중된 활동)
  - 카페: 200m (좁은 범위)
  - 운동시설: 800m (넓은 영향 범위)
  - 강의실: 400m

### GPT 요청 형식
```
다음은 {랜드마크명} 주변 {반경}m 이내의 최근 게시글들입니다.
캠퍼스 학생들이 이 장소 주변에서 어떤 분위기인지 친근하고 유익한 톤으로 3-4줄 요약해주세요.

주의사항:
- 광고성, 스팸성, 부적절한 내용은 제외하고 요약해 주세요
- 학생들의 실제 캠퍼스 생활과 관련된 내용만 반영해 주세요
- 감정이나 키워드를 중심으로 현재 분위기를 전달해 주세요

게시글 목록:
title: {제목1}
contents: {내용1}

title: {제목2}  
contents: {내용2}
...
```

### 데이터 저장 및 캐싱 전략
- **DB 저장**: 생성된 요약을 Landmark 엔티티에 영구 저장
- **Redis 캐싱**: 최신 요약을 Redis에 캐싱하여 빠른 조회 제공
  - Key: `landmark:summary:{landmarkId}`
  - TTL: 30분 (배치 주기와 동일)
  - Cache Miss 시 DB에서 조회 후 캐싱

### 데이터 품질 관리
- **게시글 길이 필터링**: 10자 미만의 짧은 게시글 제외
- **특수문자 비율 체크**: 특수문자/이모지 비율 50% 이상인 게시글 제외
- **GPT 전처리**: 요약 요청 시 부적절한 내용 제외 지시문 포함

### 배치 작업 모니터링
- 실행 시간, 성공/실패 로그 기록
- 실패 시 관리자 알림 (Slack/Email)
- 요약 생성 실패 시 이전 요약 유지

## 에러 코드 정의
- `400`: 잘못된 요청 (필수 파라미터 누락, 잘못된 좌표)
- `401`: 인증 실패 (관리자 권한 필요)
- `404`: 랜드마크를 찾을 수 없음
- `409`: 중복된 랜드마크 (같은 위치에 이미 존재)
- `500`: 서버 내부 오류 (GPT API 호출 실패 등)

## 카테고리 정의
```java
public enum LandmarkCategory {
    LIBRARY("도서관"),
    CAFE("카페"), 
    CLASSROOM("강의실"),
    SPORTS("운동시설"),
    RESTAURANT("식당"),
    DORMITORY("기숙사"),
    CONVENIENCE("편의시설"),
    ETC("기타");
}
```

## 보안 설정
- ChatGPT API Key는 환경변수 `OPENAI_API_KEY`로 관리
- 랜드마크 CUD 작업은 관리자 권한 필요
- 요약 조회는 인증된 사용자만 가능