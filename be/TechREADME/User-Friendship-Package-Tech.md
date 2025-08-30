# User & Friendship 패키지 기술 문서

## 개요
User와 Friendship 패키지는 사용자 인증과 소셜 네트워킹 기능을 담당하며, 보안성과 확장성을 고려한 설계를 구현했습니다.

## 주요 기술적 특징

### 1. 보안 강화 인증 시스템 ⭐

**암호화 기술**: SHA-256 + Base64 인코딩

**핵심 구현**:
```java
// UserService.java:141-150
private String sha256Base64(String raw) {
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    } catch (Exception e) {
        throw new RuntimeException("비밀번호 해시에 실패했습니다", e);
    }
}

// 로그인 시 해시 검증
String reqHash = sha256Base64(request.getPassword().trim());
if (!reqHash.equals(user.getPasswordHash())) {
    return new LoginResponse(false, "비밀번호가 일치하지 않습니다");
}
```

**보안 특징**:
- **단방향 암호화**: SHA-256으로 비밀번호 원본 저장 방지
- **솔트 확장 가능**: 향후 랜덤 솔트 추가 가능한 구조
- **에러 핸들링**: 암호화 실패 시 명확한 예외 처리

### 2. FCM 토큰 자동 갱신 시스템

**실시간 푸시 준비**:
```java
// UserService.java:56-61
// FCM 토큰이 전달되면 자동 업데이트
if (request.getFcmToken() != null && !request.getFcmToken().trim().isEmpty()) {
    user.setFcmToken(request.getFcmToken().trim());
    userRepository.save(user);
}
```

**기술적 장점**:
- **자동 갱신**: 로그인 시 FCM 토큰 자동 업데이트
- **Null Safe**: 안전한 공백 및 null 처리
- **확장성**: 추후 다중 디바이스 토큰 관리 확장 가능

### 3. 양방향 친구 관계 시스템 ⭐

**무결성 보장 로직**:
```java
// FriendshipService.java:39-48
// 양방향 중복 확인으로 데이터 무결성 보장
Optional<Friendship> existingFriendship = friendshipRepository.findByRequesterAndAddressee(
        requester, targetUser);

Optional<Friendship> reverseExistingFriendship = friendshipRepository.findByRequesterAndAddressee(
        targetUser, requester);

if (existingFriendship.isPresent() || reverseExistingFriendship.isPresent()) {
    throw new IllegalStateException("이미 친구이거나 요청이 존재합니다.");
}
```

**상태 관리**:
```java
// FriendshipService.java:92-105
if (!"pending".equals(friendship.getStatus())) {
    if ("accepted".equals(friendship.getStatus())) {
        // 이미 수락된 경우 멱등성 보장
        return FriendshipDto.builder()...build();
    } else {
        throw new IllegalStateException("이미 처리된 요청입니다.");
    }
}
```

### 4. 통합 알림 시스템

**자동 알림 생성**:
```java
// FriendshipService.java:58-67
Notification notification = Notification.builder()
        .user(targetUser)
        .type("friend_request")
        .title("새로운 친구 요청")
        .message(requester.getNickname() + "님이 친구 요청을 보냈습니다.")
        .data("{\"friendshipId\":" + friendship.getFriendshipId() + 
              ",\"requesterId\":\"" + requester.getUserId() + "\"}")
        .build();
```

**스마트 알림 관리**:
```java
// FriendshipService.java:111-117
// 처리 완료 시 기존 알림 자동 읽음 처리
notificationRepository.findByUser_UserIdAndTypeAndDataContaining(
        userId, "friend_request", "\"friendshipId\":" + friendshipId)
        .forEach(existingNotification -> {
            existingNotification.setIsRead(true);
            notificationRepository.save(existingNotification);
        });
```

### 5. 권한 기반 접근 제어

**세밀한 권한 검증**:
```java
// FriendshipService.java:86-89
// 요청 수신자만 수락 가능
if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
    throw new IllegalArgumentException("요청을 수락할 권한이 없습니다.");
}

// FriendshipService.java:228-232
// 친구 관계 당사자만 해제 가능
if (!friendship.getRequester().getId().equals(currentUser.getId()) &&
        !friendship.getAddressee().getId().equals(currentUser.getId())) {
    throw new IllegalArgumentException("친구 관계를 해제할 권한이 없습니다.");
}
```

### 6. 동적 친구 목록 조회

**유연한 관계 매핑**:
```java
// FriendshipService.java:204-208
// 요청자/수신자 구분 없이 상대방 정보 반환
User friend = friendship.getRequester().getId().equals(user.getId())
        ? friendship.getAddressee()  // 내가 요청자면 수신자가 친구
        : friendship.getRequester(); // 내가 수신자면 요청자가 친구
```

### 7. 입력 검증 및 예외 처리

**포괄적 검증**:
```java
// UserService.java:38-43
if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
    return new LoginResponse(false, "사용자 ID를 입력해주세요");
}
if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
    return new LoginResponse(false, "비밀번호를 입력해주세요");
}
```

**자기 참조 방지**:
```java
// FriendshipService.java:34-37
if (requester.getId().equals(targetUser.getId())) {
    throw new IllegalStateException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
}
```

## 성능 최적화 포인트

1. **인덱스 활용**: userId, friendshipId 기반 빠른 조회
2. **배치 조회**: 친구 목록 일괄 조회로 N+1 문제 방지
3. **상태 기반 필터링**: 데이터베이스 레벨에서 status 필터링
4. **트림 처리**: 입력값 정규화로 중복 데이터 방지

## 확장성 고려사항

- **JWT 토큰**: 현재 userId 기반에서 JWT로 업그레이드 예정
- **다중 디바이스**: FCM 토큰 배열로 확장 가능
- **소셜 기능**: 친구 추천, 그룹 채팅 등 확장 기능 지원
- **OAuth 연동**: 소셜 로그인 통합 가능한 구조

## 보안 고려사항

- **비밀번호 정책**: 복잡도 검증 추가 권장
- **세션 관리**: 토큰 만료 및 갱신 메커니즘 도입
- **Rate Limiting**: 친구 요청 스팸 방지
- **개인정보 보호**: GDPR 준수를 위한 데이터 삭제 정책

이러한 설계를 통해 **보안성**, **확장성**, **사용자 경험**을 모두 고려한 견고한 소셜 네트워킹 플랫폼을 구축했습니다.