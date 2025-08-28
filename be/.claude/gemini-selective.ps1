# Gemini 선택적 파일 분석 스크립트
# PowerShell 버전 - 더 정교한 파일 선택 및 컨텍스트 관리

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("analyze", "search", "document")]
    [string]$Mode,
    
    [Parameter(Mandatory=$true)]
    [string]$Query,
    
    [Parameter(Mandatory=$false)]
    [string[]]$Files = @(),
    
    [Parameter(Mandatory=$false)]
    [string]$Path = "src",
    
    [Parameter(Mandatory=$false)]
    [int]$MaxTokens = 4000
)

function Write-Header {
    param([string]$Title)
    Write-Host "=" * 50 -ForegroundColor Cyan
    Write-Host $Title -ForegroundColor Yellow
    Write-Host "=" * 50 -ForegroundColor Cyan
}

function Get-FileSize {
    param([string]$FilePath)
    if (Test-Path $FilePath) {
        return (Get-Content $FilePath -Raw).Length
    }
    return 0
}

function Select-OptimalFiles {
    param([string[]]$FilePaths, [int]$MaxSize)
    
    $selectedFiles = @()
    $totalSize = 0
    
    foreach ($file in $FilePaths) {
        $fileSize = Get-FileSize $file
        if (($totalSize + $fileSize) -le $MaxSize) {
            $selectedFiles += $file
            $totalSize += $fileSize
            Write-Host "✓ 포함: $file ($fileSize bytes)" -ForegroundColor Green
        } else {
            Write-Host "✗ 제외: $file (크기 초과)" -ForegroundColor Red
        }
    }
    
    Write-Host "총 선택된 파일: $($selectedFiles.Count), 총 크기: $totalSize bytes" -ForegroundColor Cyan
    return $selectedFiles
}

Write-Header "Gemini 선택적 파일 분석 도구"

switch ($Mode) {
    "analyze" {
        Write-Host "[분석 모드] 선택된 파일들을 분석합니다..." -ForegroundColor Yellow
        
        if ($Files.Count -eq 0) {
            Write-Host "분석할 파일을 지정해주세요." -ForegroundColor Red
            Write-Host "예: -Files @('UserController.java', 'EmotionService.java')"
            exit 1
        }
        
        $selectedFiles = Select-OptimalFiles $Files $MaxTokens
        
        if ($selectedFiles.Count -eq 0) {
            Write-Host "분석할 수 있는 파일이 없습니다." -ForegroundColor Red
            exit 1
        }
        
        # 파일 내용을 임시 파일에 결합
        $tempFile = [System.IO.Path]::GetTempFileName()
        $combinedContent = @()
        
        foreach ($file in $selectedFiles) {
            if (Test-Path $file) {
                $combinedContent += "=== $file ==="
                $combinedContent += Get-Content $file
                $combinedContent += ""
            }
        }
        
        $combinedContent | Out-File -FilePath $tempFile -Encoding UTF8
        
        Write-Host "Gemini로 분석 중..." -ForegroundColor Green
        Write-Host "명령어: Get-Content '$tempFile' | gemini --all-files=false -p '$Query'"
        
        # 실제 실행 (할당량 복구 시 사용)
        # Get-Content $tempFile | gemini --all-files=false -p $Query
        
        Remove-Item $tempFile
    }
    
    "search" {
        Write-Host "[검색 모드] 로컬 검색 후 선택적 Gemini 분석..." -ForegroundColor Yellow
        
        # ripgrep로 검색
        Write-Host "로컬 검색 실행 중..." -ForegroundColor Cyan
        $searchResults = rg $Query $Path --type java --type js --type json -n -C 1 2>$null
        
        if ($searchResults) {
            Write-Host "검색 결과:" -ForegroundColor Green
            $searchResults
            
            Write-Host "`n[Gemini 분석을 원하시면 다음 명령어 사용]:" -ForegroundColor Yellow
            Write-Host "echo '$($searchResults -join "`n")' | gemini --all-files=false -p '이 검색 결과를 분석해주세요: $Query'" -ForegroundColor Cyan
        } else {
            Write-Host "검색 결과가 없습니다." -ForegroundColor Red
        }
    }
    
    "document" {
        Write-Host "[문서 생성 모드] 선택적 파일들의 문서를 생성합니다..." -ForegroundColor Yellow
        
        if ($Files.Count -eq 0) {
            # 자동으로 컨트롤러 파일들 찾기
            $Files = Get-ChildItem -Path $Path -Recurse -Filter "*Controller.java" | Select-Object -ExpandProperty FullName
            Write-Host "자동 선택된 Controller 파일들:" -ForegroundColor Cyan
            $Files | ForEach-Object { Write-Host "  - $_" }
        }
        
        $selectedFiles = Select-OptimalFiles $Files $MaxTokens
        
        # 문서 템플릿 생성
        $docContent = @()
        $docContent += "# API 문서 - 자동 생성"
        $docContent += "생성일시: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        $docContent += ""
        
        foreach ($file in $selectedFiles) {
            if (Test-Path $file) {
                $fileName = Split-Path $file -Leaf
                $docContent += "## $fileName"
                $docContent += ""
                $docContent += "```java"
                $docContent += Get-Content $file
                $docContent += "```"
                $docContent += ""
            }
        }
        
        $outputFile = "generated-api-docs.md"
        $docContent | Out-File -FilePath $outputFile -Encoding UTF8
        
        Write-Host "기본 문서가 생성되었습니다: $outputFile" -ForegroundColor Green
        Write-Host "`n[Gemini로 고품질 문서 생성을 원하시면 다음 명령어 사용]:" -ForegroundColor Yellow
        Write-Host "Get-Content '$outputFile' | gemini --all-files=false -p 'API 문서를 개선하고 상세한 설명을 추가해주세요'" -ForegroundColor Cyan
    }
}

Write-Host "`n할당량 효율화 팁:" -ForegroundColor Magenta
Write-Host "1. --all-files=false 플래그 필수 사용" -ForegroundColor White
Write-Host "2. 파이프라인으로 컨텍스트 제한" -ForegroundColor White  
Write-Host "3. 구체적이고 명확한 질문 작성" -ForegroundColor White
Write-Host "4. 로컬 도구 우선 활용 후 AI 보완" -ForegroundColor White