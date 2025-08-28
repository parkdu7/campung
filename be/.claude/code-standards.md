# 코딩 표준

## Spring Boot 관련

### 어노테이션 사용
```java
@Service        // 비즈니스 로직 담당
@Repository     // 데이터 액세스 담당  
@Controller     // 웹 요청 처리 담당
@RestController // REST API 담당
@Component      // 일반적인 스프링 빈
```

### 의존성 주입
- **Constructor Injection 사용** (Field Injection 금지)
- `@RequiredArgsConstructor` 활용
- `@Autowired` 사용 금지

```java
@Service
@RequiredArgsConstructor
public class SampleService {
    private final SampleRepository repository;
    private final AnotherService anotherService;
}
```

### 설정 관리
- `application.yml` 사용 (`.properties` 대신)
- 환경별 프로파일 분리
- 민감한 정보는 환경변수 사용

## 예외 처리 표준

### 커스텀 예외 사용
```java
// Bad
try {
    // some logic
} catch (Exception e) {
    log.error("Error occurred", e);
}

// Good  
public void someMethod() {
    // logic that might throw custom exception
    if (condition) {
        throw new CustomBusinessException("Business rule violated");
    }
}
```

### 예외 클래스 구조
```java
public class BusinessException extends RuntimeException {
    private final String errorCode;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

## 감정 분석 관련 표준

### GPT-5 API 사용
```java
// 요청 바디 구성
Map<String, Object> requestBody = new HashMap<>();
requestBody.put("model", "gpt-5-mini");
requestBody.put("messages", List.of(
    Map.of("role", "user", "content", prompt)
));
requestBody.put("reasoning_effort", "medium");
requestBody.put("verbosity", "low");
```

### 배치 처리 패턴
```java
private static final int MAX_BATCH_SIZE = 30;

public Map<String, Integer> processBatch(List<PostData> posts) {
    if (posts.size() <= MAX_BATCH_SIZE) {
        return analyzeSingleBatch(posts);
    }
    return processInChunks(posts);
}
```

### Redis 키 패턴
```java
private static final String REDIS_SCORE_KEY = "emotion:daily:scores:";
private static final String REDIS_COUNT_KEY = "emotion:daily:count:";
private static final String REDIS_WEATHER_KEY = "emotion:daily:weather";
```

## 아키텍처 패턴

### 서비스 분리 원칙
```java
// API 통신 전담
@Service
public class GPT5ApiService { }

// 프롬프트 생성 전담  
@Service
public class EmotionPromptBuilder { }

// 응답 파싱 전담
@Service
public class EmotionResponseParser { }

// 오케스트레이션
@Service  
public class EmotionAnalysisService {
    private final GPT5ApiService apiService;
    private final EmotionPromptBuilder promptBuilder;
    private final EmotionResponseParser responseParser;
}
```

### DTO 설계
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto {
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("data")
    private DataObject data;
}
```

## 데이터베이스 관련

### Entity 설계
```java
@Entity
@Table(name = "content")
@Getter
@NoArgsConstructor
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contentId;
    
    @Column(nullable = false)
    private String title;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Repository 패턴
```java
@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    List<Content> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime startTime, LocalDateTime endTime);
}
```

## 로깅 표준

### 로그 레벨 사용
```java
log.info("비즈니스 로직 실행: {}", businessData);    // 정상 흐름
log.warn("예상치 못한 상황 발생: {}", warningData);   // 경고
log.error("오류 발생: {}", errorData);             // 에러
```

### 로그 메시지 형식
```java
// Good
log.info("감정 분석 시작: {}개 게시글", posts.size());
log.info("GPT-5 API 호출 성공, 토큰 사용량: {}", tokens);

// Bad  
log.info("starting emotion analysis");
log.info("api call finished");
```

## 성능 최적화

### 배치 처리
- 30개 단위로 API 호출 분할
- 청크별 병렬 처리 고려
- 대용량 데이터 스트림 처리

### 캐싱 전략  
- Redis를 통한 통계 데이터 캐싱
- 일일 단위 TTL 설정
- 자주 조회되는 데이터 우선 캐싱

### 시간대 처리
```java
private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
LocalDateTime now = LocalDateTime.now(KST_ZONE);
```

## API 설계

### RESTful URL 패턴
```
GET  /api/emotion/statistics      # 통계 조회
POST /api/emotion/analyze         # 수동 분석
POST /api/emotion/analyzeAll      # 전체 분석  
POST /api/emotion/reset           # 데이터 초기화
```

### 응답 형식 통일
```json
{
  "success": true,
  "message": "작업 완료 메시지",
  "data": {
    // 실제 데이터
  }
}
```

## 테스트 관련

### 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
    
    @Test
    void should_return_expected_result_when_valid_input() {
        // given
        // when  
        // then
    }
}
```

### 통합 테스트
```java
@SpringBootTest
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13");
}
```