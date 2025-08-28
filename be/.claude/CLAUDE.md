# Campung Backend Development Guide

## 개발 가이드라인

### 1. 예외 처리 (Exception Handling)

**✅ 올바른 방식 - 글로벌 예외 처리 핸들러 사용:**
```java
// Service Layer에서 비즈니스 예외 발생
if (user == null) {
    throw new UserNotFoundException("사용자를 찾을 수 없습니다");
}

// GlobalExceptionHandler에서 일괄 처리
@ExceptionHandler(UserNotFoundException.class)
public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
    return ResponseEntity.status(404).body(new ErrorResponse(false, ex.getMessage()));
}
```

**❌ 지양할 방식 - try-catch 사용:**
```java
// Controller에서 직접 try-catch 사용하지 말 것
try {
    UserResponse user = userService.getUser(id);
    return ResponseEntity.ok(user);
} catch (Exception e) {
    return ResponseEntity.status(500).body(new ErrorResponse(false, "서버 오류"));
}
```

### 2. 글로벌 예외 처리 핸들러 확장

현재 프로젝트의 `GlobalExceptionHandler`가 부족한 경우 다음 예외들을 추가하세요:

```java
// 공통 예외들
- ValidationException: 입력값 검증 실패
- UnauthorizedException: 인증 실패
- ForbiddenException: 권한 없음
- ResourceNotFoundException: 리소스 없음
- DuplicateResourceException: 중복 리소스
- BusinessLogicException: 비즈니스 로직 오류
```

### 3. 개발 완료 후 필수 검증 절차

모든 기능 개발이 완료되면 반드시 다음 순서로 검증하세요:

```bash
# 1. 컴파일 검증
./gradlew compileJava

# 2. 빌드 검증
./gradlew build

# 3. 빌드 결과 확인
# build/libs/ 디렉토리에 jar 파일 생성 확인
```

### 4. 코드 작성 규칙

- **예외 처리**: 비즈니스 로직에서는 적절한 커스텀 예외를 throw하고, GlobalExceptionHandler에서 처리
- **응답 형식**: 일관된 응답 DTO 구조 사용 (success, message, data 필드)
- **로깅**: 중요한 비즈니스 로직에는 적절한 로그 추가
- **검증**: 입력값 검증은 @Valid 어노테이션과 커스텀 예외 활용

### 5. Swagger 문서화

- 모든 Controller에 적절한 `@Tag` 어노테이션 추가
- 인증이 필요한 API에는 `@Operation(security = @SecurityRequirement(name = "bearerAuth"))` 추가
- Health Check 등 불필요한 API는 `@Hidden` 어노테이션으로 제외

### 6. 개발 워크플로우

1. 기능 개발
2. 커스텀 예외 정의 (필요시)
3. 글로벌 예외 핸들러에 예외 처리 추가
4. Swagger 문서 확인
5. **컴파일 및 빌드 검증** ← 필수
6. 사용자에게 실행 가이드 제공

이 가이드를 따라 개발하면 일관성 있고 안정적인 코드를 작성할 수 있습니다.