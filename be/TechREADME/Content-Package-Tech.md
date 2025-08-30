# Content 패키지 기술 문서

## 개요
Content 패키지는 캠퍼스 게시글 관리의 핵심 기능을 담당하며, 일반 CRUD를 넘어 다양한 고급 기술들을 활용하여 사용자 경험을 향상시킵니다.

## 주요 기술적 특징

### 1. Redis 기반 HOT 게시글 시스템 ⭐

**기술 스택**: Redis, Scheduler, Real-time Processing

**핵심 구현**:
```java
// ContentHotService.java:36-73
@Transactional
public void updateHotContent() {
    // 기존 좋아요 데이터를 Redis로 마이그레이션
    contentHotTrackingService.migrateExistingLikesToRedis();
    
    // Redis에서 상위 10개 HOT 컨텐츠 가져오기
    Set<Object> hotContentIds = contentHotTrackingService.getTop10HotContent();
    
    // 24시간 기준 좋아요 수 집계로 HOT 점수 계산
    for (Object contentIdObj : hotContentIds) {
        Long contentId = Long.valueOf(contentIdObj.toString());
        long hotScore = contentHotTrackingService.getLike24hCount(contentId);
        
        if (hotScore >= 5) { // 최소 임계값 5개
            // ContentHot 엔티티 생성 및 저장
        }
    }
}
```

**기술적 특징**:
- **실시간 집계**: Redis Sorted Set을 활용한 24시간 슬라이딩 윈도우
- **배치 처리**: 스케줄러를 통한 주기적 HOT 게시글 업데이트
- **캐시 효율**: 자주 조회되는 인기 게시글을 메모리에 캐싱

### 2. 05시 기준 캠퍼스 사이클 시스템

**핵심 로직**:
```java
// ContentHotService.java:142-153
private LocalDateTime getCurrentCycleStart() {
    LocalDateTime now = LocalDateTime.now();
    LocalTime fiveAm = LocalTime.of(5, 0);
    
    if (now.toLocalTime().isBefore(fiveAm)) {
        return now.minusDays(1).with(fiveAm); // 전날 05:00부터
    } else {
        return now.with(fiveAm); // 당일 05:00부터
    }
}
```

**기술적 목적**: 캠퍼스 생활 패턴에 맞춘 게시글 라이프사이클 관리

### 3. 썸네일 자동 생성 시스템

**기술 스택**: AWS S3, 이미지 처리, 비동기 처리

**핵심 기능**:
- **자동 리사이징**: 업로드된 이미지의 썸네일 자동 생성
- **S3 통합**: 원본과 썸네일을 분리된 경로에 저장
- **메타데이터 관리**: Content와 Attachment 엔티티 간 연관관계 관리

### 4. 파일 검증 및 보안

**구현된 검증**:
- **파일 크기 제한**: FileSizeValidationService를 통한 용량 검증
- **MIME 타입 검증**: 허용된 파일 형식만 업로드 가능
- **보안 스캔**: 악성 파일 업로드 방지

### 5. 검색 최적화

**기술적 구현**:
- **Full-Text Search**: 제목, 내용, 건물명 통합 검색
- **필터링**: 감정, 날짜, 건물별 다중 필터 지원
- **페이징**: 대용량 데이터 효율적 조회

## 성능 최적화 포인트

1. **Redis 캐싱**: HOT 게시글 조회 성능 향상
2. **배치 처리**: 실시간이 아닌 스케줄링을 통한 부하 분산
3. **썸네일 캐싱**: CDN과 연동하여 이미지 로딩 속도 최적화
4. **인덱싱**: 검색 쿼리 최적화를 위한 DB 인덱스 활용

## 확장성 고려사항

- **샤딩 준비**: 대용량 게시글 처리를 위한 DB 분할 고려
- **CDN 연동**: 전역 썸네일 배포를 위한 CloudFront 활용 가능
- **캐시 전략**: Redis 외 추가 캐시 레이어 도입 가능

이러한 기술들을 통해 단순한 게시글 시스템을 넘어 실시간 인기도 반영, 효율적인 미디어 처리, 고성능 검색이 가능한 플랫폼으로 발전시켰습니다.