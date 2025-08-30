# Notification 패키지 기술 문서

## 개요
Notification 패키지는 실시간 알림 시스템을 담당하며, Firebase Cloud Messaging과 WebSocket을 활용한 다채널 알림 전송과 지리적 기반 실시간 이벤트 브로드캐스팅을 구현했습니다.

## 주요 기술적 특징

### 1. 다채널 통합 알림 시스템 ⭐

**이중 저장 전략**: DB + FCM 이중 알림

**핵심 구현**:
```java
// NotificationService.java:117-132
@Transactional
public void createNotification(User targetUser, String type, String title, String message, String data) {
    // 1. 데이터베이스에 알림 저장 (영구 보관)
    Notification notification = Notification.builder()
            .user(targetUser)
            .type(type)
            .title(title)
            .message(message)
            .data(data)
            .isRead(false)
            .build();
    notificationRepository.save(notification);
    
    // 2. FCM 푸시 알림 발송 (즉시 전달)
    sendFCMNotification(targetUser.getFcmToken(), title, message, data);
}
```

**기술적 장점**:
- **영구성**: DB 저장으로 알림 히스토리 유지
- **즉시성**: FCM을 통한 실시간 푸시 알림
- **복원성**: 앱 재실행 시에도 읽지 않은 알림 확인 가능

### 2. 스마트 알림 필터링 시스템

**읽지 않은 알림 최적화**:
```java
// NotificationService.java:44-46
// 읽지 않은 알림만 조회로 불필요한 데이터 전송 방지
var notificationPage = notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
long unreadCount = notificationPage.getTotalElements(); // 실시간 미읽음 개수
```

**사용자별 알림 설정**:
```java
// NotificationService.java:84-114
public void updateNotificationSettings(String userId, NotificationSettingsRequest request) {
    // 동적 설정 업데이트 (null이 아닌 값만)
    if (request.getLikes() != null) {
        setting.setLikes(request.getLikes());
    }
    if (request.getComments() != null) {
        setting.setComments(request.getComments());
    }
    if (request.getLocation() != null) {
        setting.setLocation(request.getLocation());
    }
}
```

### 3. WebSocket 기반 실시간 이벤트 브로드캐스팅 ⭐

**Geohash 기반 지역별 브로드캐스팅**:
```java
// PostEventPublisher.java:18-33
public void publishNewPost(long postId, double lat, double lon) {
    String cell = geohash.geohash8(lat, lon); // 8자리 정밀도 (약 40m)
    var event = new NewPostEvent(postId, lat, lon, System.currentTimeMillis());
    
    // 3x3 그리드 (중심 + 8개 이웃 = 약 100m 반경)
    for (String neighborCell : geohash.neighbors3x3(cell)) {
        String topic = "/topic/newpost/" + neighborCell;
        broker.convertAndSend(topic, event);
    }
}
```

**실시간 이벤트 특징**:
- **지역 기반**: 사용자 위치 주변에만 관련 알림 전송
- **확장된 범위**: 3x3 그리드로 경계 지역 커버리지 보장
- **실시간성**: WebSocket을 통한 즉시 이벤트 전파

### 4. Firebase 채널별 알림 관리

**채널 기반 분류**:
```java
// NotificationService.java:152-158
AndroidConfig androidConfig = AndroidConfig.builder()
        .setNotification(AndroidNotification.builder()
                .setTitle(title)
                .setBody(message)
                .setChannelId("default_channel")  // 일반 알림
                .build())
        .build();

// FCMService에서 위치 공유용
.setChannelId("location_share_channel")  // 위치 공유 전용
```

**구조화된 데이터 전송**:
```java
// NotificationService.java:141-145
Map<String, String> dataMap = new HashMap<>();
dataMap.put("type", "normal");
if (data != null) {
    dataMap.put("data", data); // JSON 형태의 추가 데이터
}
```

### 5. 견고한 예외 처리 및 로깅

**Graceful Degradation**:
```java
// NotificationService.java:134-138
if (fcmToken == null || fcmToken.trim().isEmpty()) {
    log.warn("FCM token is null or empty, skipping push notification");
    return; // FCM 실패 시에도 DB 알림은 유지
}

// PostEventPublisher.java:30-32
} catch (Exception e) {
    log.error("Failed to publish new post event: postId={}, centerCell={}", postId, cell, e);
    // WebSocket 실패 시에도 서비스 중단 없음
}
```

### 6. 페이징 기반 효율적 데이터 로딩

**성능 최적화 조회**:
```java
// NotificationService.java:37-61
public NotificationListResponse getNotifications(String userId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    
    // 페이징된 결과와 메타데이터 함께 반환
    return NotificationListResponse.builder()
            .notifications(notifications)
            .unreadCount(unreadCount)
            .totalPages(notificationPage.getTotalPages())
            .totalElements(notificationPage.getTotalElements())
            .currentPage(page)
            .size(size)
            .build();
}
```

### 7. 상태 관리 및 사용자 행동 추적

**읽음 상태 관리**:
```java
// NotificationService.java:64-73
@Transactional
public void markAsRead(String userId, Long notificationId) {
    Notification notification = notificationRepository.findByNotificationIdAndUser_UserId(notificationId, userId)
            .orElseThrow(() -> new RuntimeException("Notification not found or access denied"));
    
    if (!notification.getIsRead()) {
        notification.setIsRead(true); // 멱등성 보장
        notificationRepository.save(notification);
    }
}
```

## 성능 최적화 포인트

1. **선택적 조회**: 읽지 않은 알림만 조회로 네트워크 트래픽 절약
2. **지역 기반 필터링**: Geohash를 통한 관련 사용자에게만 전송
3. **비동기 처리**: FCM과 WebSocket 전송의 비차단 처리
4. **페이징**: 대용량 알림 목록의 효율적 로딩

## 확장성 고려사항

- **토픽 구독**: Firebase Topic을 통한 그룹 알림 확장
- **실시간 채팅**: 기존 WebSocket 인프라 활용한 채팅 기능
- **ML 기반 필터링**: 사용자 관심사 기반 알림 우선순위 조정
- **다중 디바이스**: 사용자당 여러 FCM 토큰 관리

## 모니터링 및 분석

- **전송률 추적**: FCM 성공/실패율 모니터링
- **사용자 참여도**: 알림 클릭률 및 읽음률 분석
- **지역별 활동도**: Geohash별 이벤트 발생 빈도 추적
- **성능 메트릭**: WebSocket 연결 수 및 메시지 처리량 모니터링

이러한 기술들을 통해 **실시간성**, **지역 맞춤화**, **사용자 경험**을 모두 고려한 차세대 알림 플랫폼을 구축했습니다.