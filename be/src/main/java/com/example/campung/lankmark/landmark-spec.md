# Landmark 기능 명세서

## 개요
대학 캠퍼스 지도 기반 커뮤니티에서 중요한 장소(랜드마크)를 관리하고, 해당 위치의 실시간 분위기를 AI로 요약하는 기능

**주요 특징:**
- Form Data 기반 이미지 업로드 지원
- 자동 썸네일 생성 (300x300, JPG)  
- GlobalExceptionHandler를 통한 통합 예외 처리
- Content 업로드와 동일한 S3 이미지 처리 로직 사용

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
    private String currentSummary;    // 현재 요약
    private LocalDateTime summaryUpdatedAt; // 요약 업데이트 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## API 명세서

### 1. 랜드마크 등록 (Form Data)
- **URL**: `POST /api/landmark`
- **Content-Type**: `multipart/form-data`
- **Request Parameters**:
  - `name`: 랜드마크 이름 (필수, 최대 100자)
  - `description`: 랜드마크 설명 (선택, 최대 500자)
  - `latitude`: 위도 (필수, -90 ~ 90)
  - `longitude`: 경도 (필수, -180 ~ 180) 
  - `category`: 카테고리 (필수, LandmarkCategory enum)
  - `imageFile`: 이미지 파일 (선택, MultipartFile)

- **Request Example (Form Data)**:
  ```
  name: 중앙도서관
  description: 24시간 운영하는 메인 도서관
  latitude: 37.123456
  longitude: 127.123456
  category: LIBRARY
  imageFile: [image.jpg 파일]
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

- **주요 기능**:
  - 이미지 파일 업로드 시 S3에 원본 이미지 저장
  - 자동으로 300x300 썸네일 생성 및 S3 업로드
  - 썸네일 생성 실패해도 원본 이미지는 정상 처리
  - 중복 랜드마크 검증 (100m 이내 동일 이름)

### 2. 랜드마크 수정
- **URL**: `PUT /api/landmark/{id}`
- **Content-Type**: `multipart/form-data`
- **Path Parameters**:
  - `id`: 수정할 랜드마크 ID (필수)
- **Request Parameters**:
  - `name`: 랜드마크 이름 (필수, 최대 100자)
  - `description`: 랜드마크 설명 (선택, 최대 500자)
  - `latitude`: 위도 (필수, -90 ~ 90)
  - `longitude`: 경도 (필수, -180 ~ 180)
  - `category`: 카테고리 (필수, LandmarkCategory enum)
  - `imageFile`: 새 이미지 파일 (선택, MultipartFile)

- **Request Example (Form Data)**:
  ```
  name: 중앙도서관 (수정됨)
  description: 24시간 운영하는 메인 도서관 - 리모델링 완료
  latitude: 37.123456
  longitude: 127.123456
  category: LIBRARY
  imageFile: [new_image.jpg 파일] (선택)
  ```

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

- **주요 기능**:
  - 기존 이미지 유지 또는 새 이미지로 교체
  - 새 이미지 제공 시 자동 썸네일 재생성
  - 중복 랜드마크 검증 (현재 수정 중인 랜드마크 제외)

### 3. 랜드마크 삭제
- **URL**: `DELETE /api/landmark/{id}`
- **Path Parameters**:
  - `id`: 삭제할 랜드마크 ID (필수)
- **Response**:
  ```json
  {
    "success": true,
    "data": "랜드마크가 삭제되었습니다"
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
  - 도서관(LIBRARY): 300m (집중된 학습 활동)
  - 식당(RESTAURANT): 300m (식사 관련 정보)
  - 카페(CAFE): 200m (정확한 위치의 분위기)
  - 기숙사(DORMITORY): 500m (생활권 정보)
  - 푸드트럭(FOOD_TRUCK): 150m (정확한 위치가 중요)
  - 행사(EVENT): 200m (행사장 주변 정보)
  - 대학건물(UNIVERSITY_BUILDING): 400m (교육 시설 관련)

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

## 에러 코드 정의 및 예외 처리

### HTTP 상태 코드
- `201`: 랜드마크 등록 성공
- `400`: 잘못된 요청 (필수 파라미터 누락, 잘못된 좌표)
- `401`: 인증 실패 (관리자 권한 필요)  
- `404`: 랜드마크를 찾을 수 없음
- `409`: 중복된 랜드마크 (같은 위치에 이미 존재)
- `500`: 서버 내부 오류

### 커스텀 예외 클래스
```java
// 랜드마크 찾을 수 없음
@ExceptionHandler(LandmarkNotFoundException.class)
public ResponseEntity<Map<String, Object>> handleLandmarkNotFoundException() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("success", false, "errorCode", "LANDMARK_NOT_FOUND"));
}

// 중복 랜드마크  
@ExceptionHandler(DuplicateLandmarkException.class)
public ResponseEntity<Map<String, Object>> handleDuplicateLandmarkException() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("success", false, "errorCode", "DUPLICATE_LANDMARK"));
}

// 잘못된 좌표/입력값
@ExceptionHandler(InvalidCoordinateException.class) 
public ResponseEntity<Map<String, Object>> handleInvalidCoordinateException() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("success", false, "errorCode", "INVALID_COORDINATE"));
}

// 이미지 처리 오류
@ExceptionHandler(ImageProcessingException.class)
public ResponseEntity<Map<String, Object>> handleImageProcessingException() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("success", false, "errorCode", "IMAGE_PROCESSING_ERROR"));
}
```

### 에러 응답 형식
```json
{
  "success": false,
  "errorCode": "DUPLICATE_LANDMARK", 
  "message": "같은 위치에 '중앙도서관' 랜드마크가 이미 존재합니다."
}
```

## 카테고리 정의
```java
public enum LandmarkCategory {
    LIBRARY("도서관", 300),        // 도서관 - 300m 반경
    RESTAURANT("식당", 300),       // 식당 - 300m 반경
    CAFE("카페", 200),             // 카페 - 200m 반경
    DORMITORY("기숙사", 500),       // 기숙사 - 500m 반경
    FOOD_TRUCK("푸드트럭", 150),    // 푸드트럭 - 150m 반경
    EVENT("행사", 200),            // 행사 - 200m 반경
    UNIVERSITY_BUILDING("대학건물", 400); // 대학건물 - 400m 반경
    
    private final String description;
    private final int defaultRadius; // 기본 검색 반경(미터)
}
```

### 카테고리별 특징
- **LIBRARY**: 조용한 학습 공간, 비교적 넓은 영향 범위
- **RESTAURANT**: 식사 관련 정보, 메뉴와 혼잡도 중심
- **CAFE**: 카페 분위기, 좁은 범위로 정확한 위치 정보
- **DORMITORY**: 기숙사 생활 정보, 넓은 범위의 생활권 정보
- **FOOD_TRUCK**: 이동식 판매, 정확한 위치가 중요한 좁은 범위
- **EVENT**: 임시 행사, 단기간 정보로 좁은 범위
- **UNIVERSITY_BUILDING**: 대학 건물, 강의실 등 교육 시설

## 기술 구현 세부사항

### 이미지 처리 로직
1. **업로드 경로**: 
   - 원본 이미지: `images/contents/{yyyy}/{MM}/{dd}/{UUID}.{ext}`
   - 썸네일: `thumbnails/contents/{yyyy}/{MM}/{dd}/{UUID}_thumb.jpg`

2. **썸네일 생성 규칙**:
   - 크기: 300x300 픽셀 고정
   - 형식: JPEG 
   - 품질: 고품질 렌더링 (Bilinear interpolation)
   - 배경: 흰색, 이미지 중앙 배치
   - 비율 유지하며 스케일링

3. **S3 설정**:
   - 버킷: `${S3_BUCKET_NAME}` 환경변수
   - 리전: `${AWS_REGION}` 환경변수  
   - 인증: `${AWS_ACCESS_KEY_ID}`, `${AWS_SECRET_ACCESS_KEY}`

### GlobalExceptionHandler 통합
- **패키지 범위**: `com.example.campung.lankmark.controller`
- **예외 전파**: Service → Controller → GlobalExceptionHandler
- **로깅**: 각 예외마다 적절한 로그 레벨 적용
- **응답 통일**: 모든 에러 응답 형식 표준화

### DTO 구조
```java
// Form Data 전용 DTO
@Schema(description = "랜드마크 등록 요청 (Form Data)")
public class LandmarkCreateFormRequest {
    @NotBlank @Size(max = 100)
    private String name;
    
    @Size(max = 500) 
    private String description;
    
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    @NotNull
    private LandmarkCategory category;
    
    private MultipartFile imageFile;
}
```

## 보안 설정
- ChatGPT API Key는 환경변수 `OPENAI_API_KEY`로 관리
- 랜드마크 CUD 작업은 관리자 권한 필요
- 요약 조회는 인증된 사용자만 가능
- 이미지 파일 크기 제한: Spring Boot 기본 설정 준수
- 파일 타입 검증: `image/*` 타입만 허용