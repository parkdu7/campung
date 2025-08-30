# LocationShare & GEO 패키지 기술 문서

## 개요
LocationShare와 GEO 패키지는 실시간 위치 공유 기능을 제공하며, 지리적 데이터 처리와 Firebase 기반 실시간 알림을 통합한 고도화된 시스템입니다.

## 주요 기술적 특징

### 1. Geohash 기반 지리적 데이터 처리 ⭐

**기술 스택**: GeoHash Algorithm, Spatial Indexing

**핵심 구현**:
```java
// GeohashService.java:12-21
public String geohash8(double lat, double lon) {
    return GeoHash.encodeHash(lat, lon, 8); // 8자리 정밀도
}

public Set<String> neighbors3x3(String hash8) {
    var set = new LinkedHashSet<String>();
    set.add(hash8);                    // 중심 셀
    set.addAll(GeoHash.neighbours(hash8)); // 주변 8개 셀
    return set; // 총 9개 셀 반환
}
```

**기술적 장점**:
- **공간 효율성**: 지리적 좌표를 문자열로 압축 (8자리 = 약 40m 정밀도)
- **근접 검색**: 3x3 그리드로 주변 지역 빠른 탐색
- **인덱싱 최적화**: 문자열 기반 B-Tree 인덱스 활용 가능

### 2. 실시간 위치 공유 워크플로우 ⭐

**상태 기반 프로세스 관리**:
```java
// LocationShareService.java:44-96
// 1단계: 위치 요청 생성 및 전송
LocationRequest locationRequest = LocationRequest.builder()
        .fromUser(fromUser)
        .toUser(toUser)
        .message(request.getPurpose())
        .status("pending")
        .expiresAt(LocalDateTime.now().plusHours(24)) // 24시간 자동 만료
        .build();

// 2단계: FCM 푸시 + DB 알림 이중 저장
fcmService.sendLocationShareRequest(token, nickname, purpose, requestId);
Notification notification = Notification.builder()...build();
notificationRepository.save(notification);
```

**응답 처리 메커니즘**:
```java
// LocationShareService.java:115-150
if ("accept".equals(response.getAction())) {
    // 위치 공유 생성 (5분 고정)
    LocalDateTime displayUntil = LocalDateTime.now().plusMinutes(5);
    
    LocationShare locationShare = LocationShare.builder()
            .locationRequest(locationRequest)
            .sharedLatitude(response.getLatitude())
            .sharedLongitude(response.getLongitude())
            .displayUntil(displayUntil)
            .build();
    
    // 실시간 FCM 전송 (좌표 포함)
    fcmService.sendLocationShared(token, nickname, lat, lon, null, shareId, displayUntil);
}
```

### 3. Firebase Cloud Messaging 통합 시스템 ⭐

**다중 알림 타입 지원**:
```java
// FCMService.java:19-60
public void sendLocationShareRequest(String fcmToken, String fromUserName, String message, Long shareRequestId) {
    // 구조화된 데이터 페이로드
    Map<String, String> data = new HashMap<>();
    data.put("type", "location_share_request");
    data.put("shareRequestId", String.valueOf(shareRequestId));
    data.put("action_buttons", "true"); // Action Button 활성화
    
    // Android 전용 채널 설정
    AndroidConfig androidConfig = AndroidConfig.builder()
            .setNotification(AndroidNotification.builder()
                    .setChannelId("location_share_channel")
                    .build())
            .build();
}
```

**실시간 좌표 전송**:
```java
// FCMService.java:62-95
public void sendLocationShared(String fcmToken, String userName, BigDecimal latitude, BigDecimal longitude, ...) {
    Map<String, String> data = new HashMap<>();
    data.put("type", "location_share");
    data.put("latitude", latitude.toString());      // 정확한 위도
    data.put("longitude", longitude.toString());    // 정확한 경도
    data.put("displayUntil", displayUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
}
```

### 4. 시간 기반 만료 시스템

**자동 만료 메커니즘**:
```java
// LocationShareService.java:111-113
if (locationRequest.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new RuntimeException("만료된 요청입니다");
}

// 위치 공유 시간 제한 (5분 고정)
LocalDateTime displayUntil = LocalDateTime.now().plusMinutes(5);
```

**시간 관리 특징**:
- **요청 만료**: 24시간 후 자동 만료로 스토리지 최적화
- **공유 제한**: 5분 제한으로 개인정보 보호
- **실시간 검증**: 모든 작업 시 만료 시간 검증

### 5. 배치 처리 및 성능 최적화

**다중 사용자 처리**:
```java
// LocationShareService.java:33-96
int successCount = 0;
int totalCount = request.getFriendUserIds().size();

for (String friendUserId : request.getFriendUserIds()) {
    try {
        // 개별 사용자 처리
        User toUser = userRepository.findByUserId(friendUserId)...;
        LocationRequest savedRequest = locationRequestRepository.save(locationRequest);
        
        // FCM + 알림 병렬 처리
        if (toUser.getFcmToken() != null) {
            fcmService.sendLocationShareRequest(...);
        }
        notificationRepository.save(notification);
        
        successCount++;
    } catch (Exception e) {
        log.error("Failed to send to user: {}", friendUserId, e);
        // 부분 실패 허용, 계속 진행
    }
}
```

### 6. 알림 통합 관리

**스마트 알림 처리**:
```java
// LocationShareService.java:164-170
// 처리 완료 시 기존 알림 자동 읽음 처리
notificationRepository.findByUser_UserIdAndTypeAndDataContaining(
        respondentUserId, "location_share_request", "\"shareRequestId\":" + shareRequestId)
        .forEach(existingNotification -> {
            existingNotification.setIsRead(true);
            notificationRepository.save(existingNotification);
        });
```

### 7. 견고한 에러 처리

**Graceful Degradation**:
```java
// FCMService.java:20-23, 57-59
if (fcmToken == null || fcmToken.trim().isEmpty()) {
    log.warn("FCM token is null or empty for location share request");
    return; // 조용한 실패, 서비스 중단 없음
}

try {
    String response = FirebaseMessaging.getInstance().send(fcmMessage);
    log.info("Successfully sent FCM: {}", response);
} catch (Exception e) {
    log.error("Failed to send FCM to token: {}", fcmToken, e);
    // 로그만 남기고 계속 진행
}
```

## 성능 최적화 포인트

1. **Geohash 인덱싱**: 지리적 쿼리 성능 대폭 향상
2. **배치 전송**: 다중 사용자 FCM 동시 처리
3. **자동 만료**: 24시간/5분 자동 정리로 데이터 최적화
4. **부분 실패 허용**: 일부 사용자 실패 시에도 서비스 계속

## 확장성 고려사항

- **위치 히스토리**: 위치 공유 이력 저장 및 분석
- **지오펜싱**: 특정 영역 진입/이탈 알림
- **실시간 추적**: WebSocket을 통한 실시간 위치 업데이트
- **그룹 공유**: 다중 사용자 간 동시 위치 공유

## 보안 및 개인정보 보호

- **시간 제한**: 5분 제한으로 위치 노출 최소화
- **명시적 동의**: 요청-수락 프로세스로 동의 확보
- **자동 삭제**: 만료된 위치 데이터 자동 제거
- **토큰 검증**: FCM 토큰 유효성 검증

이러한 기술들을 통해 **개인정보 보호**, **실시간 성능**, **사용자 경험**을 모두 고려한 안전하고 효율적인 위치 공유 플랫폼을 구축했습니다.