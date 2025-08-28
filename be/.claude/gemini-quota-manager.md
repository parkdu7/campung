# Gemini 할당량 최적화 가이드

## 현재 상황 분석
- **문제**: 1회 요청으로 일일 할당량 소진
- **원인**: Claude Code 환경에서 자동으로 전체 워크스페이스가 컨텍스트에 포함됨
- **해결**: 선택적 컨텍스트 사용 및 토큰 사용량 최적화

## 최적화된 Gemini 사용법

### 1. 최소 컨텍스트 모드
```bash
# 기존 (위험): 전체 프로젝트 포함
gemini -p "코드 분석해주세요"

# 최적화: 파일 컨텍스트 제외
gemini --all-files=false -p "간단한 질문입니다"
```

### 2. 선택적 파일 포함
```bash
# 특정 파일만 포함
echo "분석할 코드" | gemini --all-files=false -p "이 코드를 분석해주세요"

# 파일을 명시적으로 지정
type UserController.java | gemini --all-files=false -p "이 Controller 클래스를 분석해주세요"
```

### 3. 단계적 분석 전략
```bash
# 1단계: 로컬 도구로 예비 분석
rg "emotion" --type java -n

# 2단계: 필요한 부분만 Gemini로 분석
gemini --all-files=false -p "감정 분석 코드의 구현 패턴을 설명해주세요"
```

## 배치 스크립트 활용

### gemini-optimized.bat 사용법
```cmd
# 분석 모드 (최소 컨텍스트)
gemini-optimized.bat analyze "이 프로젝트의 주요 기능은?"

# 검색 모드 (로컬 검색 + 선택적 AI 분석)
gemini-optimized.bat search "OpenAI" src

# 문서 생성 모드 (특정 파일만)
gemini-optimized.bat document api UserController.java ContentController.java
```

## 할당량 관리 전략

### 우선순위 기반 사용
1. **높음**: 핵심 비즈니스 로직 분석
2. **중간**: API 문서 생성
3. **낮음**: 일반적인 코드 설명

### 로컬 도구 우선 활용
```bash
# 검색: ripgrep 사용
rg "pattern" --type java -n -C 2

# 파일 구조: tree 명령어
tree src -I "*.class|*.jar"

# 코드 분석: grep, awk 조합
grep -r "public.*Controller" src --include="*.java"
```

### 토큰 절약 팁
1. **구체적인 질문**: "전체 분석" 대신 "특정 기능 분석"
2. **작은 단위**: 파일별로 나누어 분석
3. **배치 처리**: 할당량 복구 후 일괄 처리

## 응급 사용법 (할당량 부족 시)

### 대체 분석 도구
```bash
# 1. 코드 메트릭
find src -name "*.java" | wc -l  # Java 파일 수
grep -r "TODO\|FIXME" src        # 개선점 찾기

# 2. 의존성 분석
grep -r "import" src --include="*.java" | sort | uniq -c

# 3. 패턴 분석
grep -r "public class.*Controller" src --include="*.java"
```

### 템플릿 기반 문서
1. API 명세: Swagger 어노테이션 기반 자동 생성
2. README: 기본 템플릿 + 수동 보완
3. 아키텍처 문서: 패키지 구조 기반 자동 생성

## 할당량 복구 후 실행 계획

### 1순위: 핵심 기능 분석
```bash
gemini-optimized.bat analyze "감정 분석 시스템의 아키텍처를 설명해주세요"
gemini-optimized.bat analyze "위치 기반 서비스의 구현 방식을 분석해주세요"
```

### 2순위: 문서 생성
```bash
gemini-optimized.bat document api EmotionController.java
gemini-optimized.bat document technical OpenAIConfig.java
```

### 3순위: 최적화 제안
```bash
gemini-optimized.bat analyze "이 코드베이스의 성능 최적화 포인트를 찾아주세요"
```

## 모니터링 및 추적

### 사용량 체크리스트
- [ ] --all-files=false 플래그 사용 확인
- [ ] 필요한 최소 컨텍스트만 포함
- [ ] 구체적이고 명확한 질문 작성
- [ ] 로컬 도구로 예비 분석 완료

### 할당량 복구 알림 설정
```bash
# Windows 작업 스케줄러로 일일 체크
# 또는 간단한 배치 파일로 테스트
echo "할당량 체크" | gemini --all-files=false -p "테스트"
```

이 가이드를 통해 할당량을 효율적으로 관리하고 Gemini의 강력한 기능을 최대한 활용할 수 있습니다.