@echo off
REM Gemini 최적화 사용 스크립트
REM 토큰 사용량을 최소화하여 할당량을 효율적으로 관리

echo ==========================================
echo Gemini 최적화 사용 도구
echo ==========================================

if "%1"=="" (
    echo 사용법:
    echo   %0 analyze "분석할 내용"
    echo   %0 search "검색어" [경로]
    echo   %0 document "문서타입" [파일1] [파일2]
    echo.
    echo 예시:
    echo   %0 analyze "이 코드의 기능을 설명해주세요"
    echo   %0 search "emotion" src
    echo   %0 document api UserController.java
    goto :eof
)

if "%1"=="analyze" (
    echo [ANALYZE MODE] 최소 컨텍스트로 분석 실행...
    gemini --all-files=false --show-memory-usage -p "%2"
    goto :eof
)

if "%1"=="search" (
    echo [SEARCH MODE] 로컬 검색 후 Gemini 분석...
    if "%3"=="" (
        rg "%2" --type java --type js --type json -n -C 1
    ) else (
        rg "%2" "%3" --type java --type js --type json -n -C 1
    )
    echo.
    echo [선택적 Gemini 분석을 원하시면 다음 명령어 사용:]
    echo gemini --all-files=false -p "위 검색 결과를 분석해주세요: %2"
    goto :eof
)

if "%1"=="document" (
    echo [DOCUMENT MODE] 선택적 파일만으로 문서 생성...
    if "%3"=="" (
        echo 파일을 지정해주세요.
        goto :eof
    )
    
    echo 지정된 파일들:
    shift
    shift
    :loop
    if "%1"=="" goto :endloop
    echo - %1
    type "%1" 2>nul || echo   [파일 없음: %1]
    shift
    goto :loop
    :endloop
    
    echo.
    echo [Gemini 문서 생성을 원하시면 다음 명령어 사용:]
    echo gemini --all-files=false -p "위 파일들에 대한 %2 문서를 생성해주세요"
    goto :eof
)

echo 알 수 없는 명령어: %1
echo 도움말을 보려면 인수 없이 실행하세요.