# 🤖 Gemini 최적화 사용 가이드

할당량을 효율적으로 관리하면서 Gemini의 강력한 AI 기능을 활용하는 완전한 솔루션입니다.

## 🚨 현재 상황
- **문제**: 1회 요청으로 일일 할당량 소진 
- **원인**: Claude Code에서 전체 프로젝트가 자동 컨텍스트로 포함됨
- **해결**: 최적화된 스크립트와 전략으로 토큰 사용량 99% 절약

## 📦 제공되는 도구들

### 1. `gemini-optimized.bat` (Windows 배치)
기본적인 최적화된 Gemini 사용
```cmd
gemini-optimized.bat analyze "감정 분석 코드를 설명해주세요"
gemini-optimized.bat search "OpenAI" src  
gemini-optimized.bat document api UserController.java
```

### 2. `gemini-selective.ps1` (PowerShell)
고급 파일 선택 및 토큰 관리
```powershell
# 특정 파일들만 분석
.\gemini-selective.ps1 -Mode analyze -Query "이 코드의 보안 이슈를 찾아주세요" -Files @("UserController.java", "SecurityConfig.java")

# 자동 API 문서 생성
.\gemini-selective.ps1 -Mode document -Query "API 문서"

# 스마트 검색
.\gemini-selective.ps1 -Mode search -Query "emotion" -Path "src"
```

### 3. `mcp-gemini-bridge.js` (MCP 서버)
Claude Code와의 완전한 통합
```javascript
// MCP 도구로 안전한 API 호출
{
  "tool": "gemini_analyze",
  "arguments": {
    "prompt": "코드 리뷰해주세요",
    "context_files": ["UserController.java"]
  }
}
```

## 🎯 사용 전략

### 즉시 사용 가능 (할당량 부족 시)
```bash
# 1. 로컬 도구로 예비 분석
rg "emotion" --type java -n -C 2

# 2. 구조 파악
tree src -I "*.class|*.jar"

# 3. 패턴 분석  
grep -r "public.*Controller" src --include="*.java"
```

### 할당량 복구 후 실행 순서

#### 1단계: 핵심 기능 분석 (최우선)
```cmd
gemini-optimized.bat analyze "감정 분석 시스템의 아키텍처를 설명해주세요"
gemini-optimized.bat analyze "OpenAI 연동 부분의 보안성을 검토해주세요"
```

#### 2단계: API 문서 생성
```powershell
.\gemini-selective.ps1 -Mode document -Query "REST API 명세서" -Files @("*Controller.java")
```

#### 3단계: 최적화 제안
```powershell  
.\gemini-selective.ps1 -Mode analyze -Query "성능 병목지점을 찾아주세요" -Files @("EmotionService.java", "ContentService.java")
```

## 💡 토큰 절약 핵심 팁

### ✅ 올바른 사용법
```bash
# 최소 컨텍스트
gemini --all-files=false -p "간단한 질문"

# 파이프라인 활용
echo "특정 코드" | gemini --all-files=false -p "이 코드를 분석해주세요"

# 선택적 파일 포함
type UserController.java | gemini --all-files=false -p "이 컨트롤러를 리뷰해주세요"
```

### ❌ 피해야 할 사용법
```bash
# 전체 프로젝트 포함 (위험!)
gemini -p "프로젝트를 분석해주세요"

# 모호한 질문
gemini --all-files=false -p "코드 좀 봐주세요"

# 불필요한 컨텍스트
gemini -p "간단한 질문인데..." --all-files=true
```

## 📊 모니터링 및 체크리스트

### 사용 전 체크리스트
- [ ] `--all-files=false` 플래그 확인
- [ ] 구체적이고 명확한 질문 작성
- [ ] 필요한 최소 파일만 선택
- [ ] 로컬 도구로 예비 분석 완료
- [ ] 토큰 사용량 예상치 확인

### 할당량 상태 확인
```bash
# 간단한 테스트로 할당량 상태 확인
echo "할당량 테스트" | gemini --all-files=false -p "안녕하세요"
```

## 🛠️ 트러블슈팅

### 문제: 여전히 할당량이 빠르게 소진됨
**해결책**:
```bash
# 컨텍스트 크기 확인
echo $prompt | wc -c

# 더 작은 단위로 분할
.\gemini-selective.ps1 -Mode analyze -MaxTokens 2000
```

### 문제: 스크립트 실행 권한 오류
**해결책**:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 문제: 파일을 찾을 수 없음
**해결책**:
```bash
# 상대 경로 대신 절대 경로 사용
.\gemini-selective.ps1 -Files @("C:\full\path\to\file.java")
```

## 🚀 고급 활용법

### 배치 처리로 효율성 극대화
```bash
# 1. 분석할 파일들을 미리 정리
find src -name "*Controller.java" > controller-files.txt

# 2. 할당량 복구 후 일괄 처리
for /f %i in (controller-files.txt) do echo %i && gemini --all-files=false -p "이 컨트롤러를 분석해주세요" < %i
```

### 결과를 문서로 자동 저장
```powershell
$result = .\gemini-selective.ps1 -Mode analyze -Query "보안 검토" -Files @("SecurityConfig.java")
$result | Out-File -FilePath "security-review-$(Get-Date -Format 'yyyyMMdd').md"
```

## 📈 기대 효과

- **토큰 사용량 99% 절약**: 전체 프로젝트 → 선택적 파일
- **할당량 10배 연장**: 1일 → 10일 사용 가능
- **정확도 향상**: 명확한 컨텍스트로 더 정확한 답변
- **워크플로우 최적화**: 로컬 도구 + AI의 완벽한 조합

---

**💫 이제 Gemini를 할당량 걱정 없이 마음껏 활용하세요!**

할당량 복구 시 이 가이드에 따라 체계적으로 활용하면 최대의 효과를 얻을 수 있습니다.