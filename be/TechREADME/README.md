# 🏫 Campung Backend

> **차세대 AI 기반 캠퍼스 커뮤니티 플랫폼**  
> 실시간 감정 분석, 위치 공유, AI 분위기 요약을 통한 스마트 캠퍼스 라이프

## 📋 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [핵심 기술적 특징](#-핵심-기술적-특징)
- [아키텍처](#-아키텍처)
- [패키지별 주요 기술](#-패키지별-주요-기술)
- [API 문서](#-api-문서)
- [설치 및 실행](#-설치-및-실행)
- [환경 변수](#-환경-변수)

## 🎯 프로젝트 소개

**Campung**은 캠퍼스 내 학생들의 일상과 감정을 AI로 분석하여, 실시간 캠퍼스 분위기를 제공하는 차세대 커뮤니티 플랫폼입니다.

### 🌟 핵심 가치

- **🤖 AI 기반 인사이트**: GPT-5를 활용한 감정 분석 및 분위기 요약
- **📍 위치 기반 서비스**: Geohash 알고리즘을 통한 정밀한 지역별 정보 제공
- **⚡ 실시간 경험**: WebSocket과 FCM을 통한 즉시 알림 시스템
- **🌡️ 캠퍼스 온도**: 게시글 감정과 활동량을 기반으로 한 실시간 캠퍼스 분위기 지표

## 🚀 주요 기능

### 1. **실시간 캠퍼스 온도 시스템**
- 게시글 감정 분석을 통한 캠퍼스 분위기 온도 계산
- 게시글 활동도 기반 동적 온도 조정
- 시간대별 예측 및 자동 보정 시스템

### 2. **AI 기반 랜드마크 분위기 분석**
- GPT-5를 활용한 건물별 실시간 분위기 요약
- 비용 최적화된 GPT-5-mini 모델 사용
- Redis 캐싱을 통한 고성능 응답

### 3. **스마트 게시글 관리**
- Redis 기반 실시간 HOT 게시글 시스템
- 캠퍼스 생활 패턴 반영 (05시 기준 사이클)
- S3 통합 썸네일 자동 생성

### 4. **실시간 위치 공유**
- Geohash 알고리즘을 통한 정밀한 위치 처리
- Firebase FCM 기반 즉시 알림
- 개인정보 보호를 위한 시간 제한 (5분)

### 5. **지능형 알림 시스템**
- 위치 기반 실시간 이벤트 브로드캐스팅
- DB + FCM 이중 알림으로 안정성 보장
- 사용자 맞춤 알림 설정

### 6. **보안 강화 인증**
- SHA-256 암호화 기반 안전한 사용자 인증
- 양방향 친구 관계 무결성 보장
- JWT 토큰 확장 준비

## 🛠 기술 스택

### **Backend Core**
- **Framework**: Spring Boot 3.x, Spring Security
- **Database**: MariaDB (Latest), Redis 7.x
- **ORM**: JPA/Hibernate with QueryDSL

### **AI & External Services**
- **AI**: OpenAI GPT-5 (Chat Completions API)
- **Cloud**: AWS S3 (파일 저장), Firebase FCM (푸시 알림)
- **GIS**: GeoHash Algorithm (davidmoten/geo)

### **Real-time & Messaging**
- **WebSocket**: Spring WebSocket, SimpMessagingTemplate
- **Push Notification**: Firebase Cloud Messaging
- **Caching**: Redis with Spring Data Redis

### **Development & Deployment**
- **Build**: Gradle, Docker Compose
- **API Documentation**: Swagger/OpenAPI 3.0
- **Monitoring**: SLF4J Logging

## ⚡ 핵심 기술적 특징

### 1. **GPT-5 기반 배치 감정 분석**
```java
// 30개씩 청크 분할로 API 호출 최적화
public Map<String, Integer> processBatchEmotions(List<PostData> posts, BatchEmotionAnalyzer analyzer) {
    if (posts.size() <= MAX_BATCH_SIZE) {
        return analyzer.analyzeSingleBatch(posts);
    }
    return processInChunks(posts, analyzer);
}
```

### 2. **Geohash 기반 공간 인덱싱**
```java
// 8자리 정밀도 (약 40m) + 3x3 주변 셀 검색
public Set<String> neighbors3x3(String hash8) {
    var set = new LinkedHashSet<String>();
    set.add(hash8);                    // 중심 셀
    set.addAll(GeoHash.neighbours(hash8)); // 주변 8개 셀
    return set; // 총 9개 셀 (약 100m 반경)
}
```

### 3. **동적 온도 조정 알고리즘**
```java
// 온도 보호 + 시간대별 패턴 + 활동도 반영
private double calculateBidirectionalAdjustment(double currentTemp, int postCount, int hour) {
    double protectionFactor = getTemperatureProtectionFactor(currentTemp, isIncreasing);
    double timeMultiplier = guideline.getIncreaseMultiplier();
    double finalRate = baseAdjustmentRate * protectionFactor * timeMultiplier;
    return currentTemp * (1 + finalRate);
}
```

### 4. **WebSocket 지역별 브로드캐스팅**
```java
// Geohash 기반 실시간 이벤트 전파
for (String neighborCell : geohash.neighbors3x3(cell)) {
    String topic = "/topic/newpost/" + neighborCell;
    broker.convertAndSend(topic, event);
}
```

## 🏗 아키텍처

```
📦 Campung Backend Architecture

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │    │   Web Clients   │    │  Admin Panel    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────────────────────────────┐
         │              API Gateway                       │
         │            (Load Balancer)                     │
         └───────────────────────────────────────────────┘
                                 │
         ┌───────────────────────────────────────────────┐
         │             Spring Boot Backend                │
         │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
         │  │   Content   │  │  Landmark   │  │   Emotion   │ │
         │  │   Package   │  │   Package   │  │   Package   │ │
         │  └─────────────┘  └─────────────┘  └─────────────┘ │
         │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
         │  │    User     │  │ LocationShare│ │Notification │ │
         │  │  Friendship │  │     GEO     │  │   Package   │ │
         │  └─────────────┘  └─────────────┘  └─────────────┘ │
         └───────────────────────────────────────────────────┘
                                 │
         ┌───────────────────────────────────────────────┐
         │              Data Layer                        │
         │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
         │  │   MariaDB   │  │    Redis    │  │  WebSocket  │ │
         │  │  (Primary)  │  │  (Cache)    │  │   Broker    │ │
         │  └─────────────┘  └─────────────┘  └─────────────┘ │
         └───────────────────────────────────────────────────┘
                                 │
         ┌───────────────────────────────────────────────┐
         │            External Services                   │
         │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
         │  │  OpenAI     │  │   AWS S3    │  │ Firebase    │ │
         │  │   GPT-5     │  │   Storage   │  │    FCM      │ │
         │  └─────────────┘  └─────────────┘  └─────────────┘ │
         └───────────────────────────────────────────────────┘
```

## 📚 패키지별 주요 기술

### 🗂 Content Package
- **Redis 기반 HOT 게시글 시스템**: 24시간 슬라이딩 윈도우
- **05시 기준 캠퍼스 사이클**: 대학 생활 패턴 반영
- **S3 통합 썸네일 시스템**: 자동 리사이징 및 CDN 준비

### 🏢 Landmark Package
- **GPT-5 분위기 분석**: 비용 최적화 GPT-5-mini 사용
- **고급 Redis 캐싱**: 파라미터별 세분화 캐싱
- **자동화 스케줄러**: 1시간마다 자동 요약 생성

### 🌡 Emotion Package
- **배치 감정 분석**: 30개씩 청크 분할 처리
- **동적 온도 관리**: 예측 기반 온도 조정
- **실시간 통계**: 시간대별 온도 변화 추적

### 👥 User & Friendship Package
- **SHA-256 보안 인증**: 단방향 암호화
- **양방향 친구 시스템**: 데이터 무결성 보장
- **FCM 토큰 자동 갱신**: 다중 디바이스 준비

### 📍 LocationShare & GEO Package
- **Geohash 공간 인덱싱**: 8자리 정밀도 (40m)
- **실시간 위치 공유**: 5분 제한 개인정보 보호
- **Firebase 통합**: 구조화된 푸시 알림

### 🔔 Notification Package
- **다채널 알림**: DB + FCM 이중 시스템
- **WebSocket 브로드캐스팅**: 지역별 실시간 이벤트
- **스마트 필터링**: 읽지 않은 알림만 조회

## 📖 API 문서

API 문서는 Swagger를 통해 자동 생성됩니다.

```
http://localhost:8080/swagger-ui/index.html
```

### 주요 API 엔드포인트

- **게시글**: `/api/content/*` - CRUD, HOT 게시글, 검색
- **랜드마크**: `/api/landmark/*` - 건물별 분위기 요약
- **감정/온도**: `/api/emotion/*` - 캠퍼스 온도 조회
- **사용자**: `/api/user/*` - 인증, 회원관리
- **친구**: `/api/friendship/*` - 친구 요청, 관리
- **위치 공유**: `/api/location-share/*` - 실시간 위치 공유
- **알림**: `/api/notification/*` - 알림 조회, 설정

## 🚀 설치 및 실행

### 사전 요구사항

```bash
# Java 17+
java -version

# Docker & Docker Compose
docker --version
docker-compose --version
```

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd campung-backend
```

### 2. Docker Compose로 데이터베이스 설정

프로젝트 루트에 `docker-compose.yml` 파일을 생성하세요:

```yaml
services:
  mariadb:
    image: mariadb:latest
    container_name: campung
    environment:
      MYSQL_ROOT_PASSWORD: campung1234
      MYSQL_DATABASE: campung
      MYSQL_USER: campung
      MYSQL_PASSWORD: campung1234
    ports:
      - "3312:3306"
    volumes:
      - mariadb_data:/var/lib/mysql

  phpmyadmin:
    image: phpmyadmin:latest
    container_name: campung-phpmyadmin
    environment:
      PMA_HOST: mariadb
      PMA_USER: campung
      PMA_PASSWORD: campung1234
      MYSQL_ROOT_PASSWORD: campung1234
    ports:
      - "9013:80"
    depends_on:
      - mariadb

  redis:
    image: redis:alpine
    ports:
      - "6380:6379"
    container_name: campung-redis
    volumes:
      - redis_data:/data
    command: [ "redis-server", "--requirepass", "campung1234" ]

volumes:
  mariadb_data:
  redis_data:
```

### 3. 환경 변수 설정

`src/main/resources/properties/env.properties` 파일을 생성하세요:

```properties
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=3312
DB_NAME=campung
DB_USERNAME=campung
DB_PASSWORD=campung1234
REDIS_HOST=localhost
REDIS_PORT=6380
REDIS_PASSWORD=campung1234

AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=ap-northeast-2
S3_BUCKET_NAME=campung-media-storage

OPENAI_API_KEY=your-openai-api-key
```

### 4. Firebase 설정

`src/main/resources/firebase-service-account.json` 파일을 생성하고, Firebase 콘솔에서 다운로드받은 서비스 계정 키를 넣으세요:

```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "your-private-key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@your-project.iam.gserviceaccount.com",
  "client_id": "your-client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-xxxxx%40your-project.iam.gserviceaccount.com"
}
```

### 5. 데이터베이스 및 Redis 시작
```bash
# Docker Compose로 MariaDB, Redis, phpMyAdmin 실행
docker-compose up -d

# 데이터베이스 연결 확인
# phpMyAdmin: http://localhost:9013 (campung/campung1234)
```

### 6. 애플리케이션 빌드 및 실행
```bash
# 의존성 설치 및 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### 7. 접속 정보
- **API 서버**: http://localhost:8080
- **Swagger 문서**: http://localhost:8080/swagger-ui/index.html
- **phpMyAdmin**: http://localhost:9013 (campung/campung1234)
- **MariaDB**: localhost:3312
- **Redis**: localhost:6380 (password: campung1234)

## ⚙️ 환경 변수

### application.yml 설정 예시

```yaml
spring:
  datasource:
    url: jdbc:mariadb://${DB_HOST:localhost}:${DB_PORT:3312}/${DB_NAME:campung}
    username: ${DB_USERNAME:campung}
    password: ${DB_PASSWORD:campung1234}
    driver-class-name: org.mariadb.jdbc.Driver
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6380}
    password: ${REDIS_PASSWORD:campung1234}
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    database-platform: org.hibernate.dialect.MariaDBDialect

server:
  port: ${SERVER_PORT:8080}

openai:
  api:
    key: ${OPENAI_API_KEY}
    url: https://api.openai.com/v1/chat/completions

aws:
  s3:
    bucket: ${S3_BUCKET_NAME:campung-media-storage}
    region: ${AWS_REGION:ap-northeast-2}
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}

firebase:
  config-path: firebase-service-account.json

app:
  default-profile-image-url: ${DEFAULT_PROFILE_IMAGE_URL:https://example.com/default.jpg}
```

### 필수 환경 변수 설정

**env.properties 파일에서 설정해야 하는 값들:**

- `OPENAI_API_KEY`: OpenAI GPT-5 API 키
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`: AWS S3 접근 키
- `S3_BUCKET_NAME`: S3 버킷 이름
- 데이터베이스 연결 정보 (이미 docker-compose에서 설정됨)
- Redis 연결 정보 (이미 docker-compose에서 설정됨)

### 파일 구조
```
src/main/resources/
├── application.yml
├── properties/
│   └── env.properties          # 환경 변수 설정
└── firebase-service-account.json  # Firebase 서비스 계정 키
```

## 📊 모니터링 및 로그

### 로그 레벨 설정
```yaml
logging:
  level:
    com.example.campung: INFO
    org.springframework.web.socket: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```
