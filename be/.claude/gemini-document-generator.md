# Campung 프로젝트 - Gemini를 활용한 문서 자동 생성 시스템

## 개요
이 문서는 Gemini API를 활용하여 캠핑/캠퍼스 소셜 플랫폼의 다양한 문서를 자동 생성하는 시스템에 대해 설명합니다.

## Gemini 활용 영역

### 1. 검색 및 탐색 (Search & Exploration)
- **코드베이스 분석**: 프로젝트 구조 자동 분석 및 설명
- **API 엔드포인트 탐색**: Spring Boot Controller 자동 문서화
- **데이터베이스 스키마 분석**: JPA Entity 관계 분석
- **의존성 분석**: build.gradle 의존성 트리 분석

### 2. 문서 작성 (Documentation Generation)  
- **API 명세서**: Swagger/OpenAPI 스타일 문서 생성
- **기술 문서**: 아키텍처, 설계 패턴, 배포 가이드
- **사용자 가이드**: 기능별 사용법, FAQ
- **개발자 가이드**: 코드 컨벤션, 기여 가이드

### 3. 코드 분석 및 리뷰
- **감정 분석 로직**: OpenAI 연동 코드 분석 및 최적화 제안
- **보안 점검**: 취약점 분석 및 개선 방안
- **성능 최적화**: 쿼리 최적화, 캐싱 전략 제안
- **테스트 커버리지**: 테스트 코드 분석 및 개선점 제안

## 현재 프로젝트 분석 결과

### 주요 기능 모듈
1. **사용자 관리**: user, friendship 패키지
2. **콘텐츠**: content, comment, record 패키지  
3. **감정 분석**: emotion 패키지 (OpenAI 연동)
4. **위치 기반 서비스**: geo, locationShare, landmark 패키지
5. **알림**: notification 패키지 (Firebase 연동)

### 테스트 데이터 분석
- 감정 분석용 샘플 데이터 81개 확인
- 감정 점수: {1}~{5} 범위로 분류
- 내용: 대학생 일상, 감정 표현, 공지사항 등 다양한 카테고리

## Gemini MCP 서버 설정

### 사용 가능한 도구들
1. **gemini_analyze**: 코드/텍스트 분석
2. **gemini_search**: 코드베이스 검색 및 탐색  
3. **gemini_document**: 문서 자동 생성

### 사용 예시

#### 1. 프로젝트 구조 분석
\`\`\`javascript
// MCP 호출 예시
{
  "tool": "gemini_analyze",
  "arguments": {
    "prompt": "Spring Boot 프로젝트 구조를 분석하고 주요 기능을 설명해주세요",
    "context_files": ["build.gradle", "src/main/java/com/example/campung/CampungApplication.java"]
  }
}
\`\`\`

#### 2. API 문서 생성
\`\`\`javascript
{
  "tool": "gemini_document", 
  "arguments": {
    "doc_type": "api",
    "source_files": [
      "src/main/java/com/example/campung/user/UserController.java",
      "src/main/java/com/example/campung/content/ContentController.java"
    ]
  }
}
\`\`\`

#### 3. 감정 분석 기능 탐색
\`\`\`javascript
{
  "tool": "gemini_search",
  "arguments": {
    "search_query": "emotion analysis OpenAI",
    "search_path": "src/main/java"
  }
}
\`\`\`

## 할당량 제한 시 대안

현재 Gemini API 할당량 초과 상황에서는 다음과 같은 대안을 활용:

1. **로컬 분석 도구**: ripgrep, 파일 시스템 분석
2. **템플릿 기반 문서**: 기본 구조 제공 후 수동 보완
3. **배치 처리**: 할당량 복구 시 일괄 처리를 위한 큐 시스템

## 향후 개선 방안

1. **실시간 문서 동기화**: 코드 변경 시 자동 문서 업데이트
2. **다국어 문서 지원**: 한국어/영어 자동 번역
3. **시각화 생성**: 아키텍처 다이어그램, 플로우차트 자동 생성
4. **품질 메트릭**: 코드 품질 지표 자동 분석 및 리포트

## 결론

Gemini를 MCP 형태로 연결하여 개발 워크플로우에 통합함으로써:
- **개발 효율성 향상**: 자동화된 문서 생성 및 코드 분석
- **품질 관리**: 일관된 문서 품질 및 코드 리뷰
- **지식 공유**: 프로젝트 이해도 향상 및 온보딩 지원

할당량 제한은 임시적인 문제이며, API 복구 시 전체 기능을 활용할 수 있습니다.