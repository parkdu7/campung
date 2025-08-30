# 캠퍼스 커뮤니티 앱 (Campung)

캠퍼스 내 위치 기반 소셜 네트워킹 앱으로, 지도를 통한 콘텐츠 공유 및 친구 위치 공유 기능을 제공합니다.

## 📱 Android 개발 환경 설정

### 필수 요구사항

- **Android Studio**: Flamingo (2022.2.1) 이상
- **JDK**: Java 8 이상
- **Android SDK**: API Level 26 (Android 8.0) 이상
- **Kotlin**: 2.0.21
- **Gradle**: 8.10.1

### 개발 환경 준비

#### 1. Android Studio 설치
1. [Android Studio 공식 사이트](https://developer.android.com/studio)에서 최신 버전 다운로드
2. 설치 시 Android SDK, Android SDK Platform, Android Virtual Device 포함 설치

#### 2. SDK 구성 요소 설치
Android Studio > SDK Manager에서 다음 구성 요소 설치:
```
- Android SDK Platform 36 (Target SDK)
- Android SDK Platform 26 (Min SDK)
- Android SDK Build-Tools 36.0.0
- Android SDK Platform-Tools
- Android SDK Tools
```

#### 3. 환경 변수 설정 (선택사항)
```bash
# ANDROID_HOME 설정
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## 🔧 프로젝트 설정

### 1. 저장소 클론
```bash
git clone <repository-url>
cd campung
```

### 2. 필수 파일 설정

#### `local.properties` 파일 생성
`android/campung/` 디렉토리에 생성:
```properties
sdk.dir=C:\\Users\\[사용자명]\\AppData\\Local\\Android\\Sdk
NAVER_MAP_CLIENT_ID=your_naver_map_client_id
```

#### `google-services.json` 파일 추가
Firebase 설정 파일을 `android/campung/app/` 디렉토리에 복사

### 3. 의존성 확인
주요 라이브러리 버전:
```kotlin
// Core
- Kotlin: 2.0.21
- Compose BOM: 2024.12.01
- Compile SDK: 36
- Min SDK: 26
- Target SDK: 36

// Architecture
- Hilt: 2.52 (Dependency Injection)
- Room: 2.6.1 (Local Database)
- Navigation Compose: 2.8.0

// Network
- Retrofit: 2.11.0
- OkHttp: 4.12.0
- WebSocket: 1.5.3

// Maps & Location
- Naver Maps SDK: 3.22.1
- Naver Map Compose: 1.7.0
- Google Play Services Location: 21.3.0

// Firebase
- Firebase BOM: 34.1.0
- Firebase Messaging (FCM)

// UI & Animation
- Material3
- Lottie: 6.6.0
- Coil: 2.7.0 (Image Loading)

// Media
- ExoPlayer: 1.4.1 (Audio/Video)
```

## 🚀 빌드 및 실행

### 1. Android Studio에서 실행
```bash
1. Android Studio 실행
2. "Open an Existing Project" 선택
3. campung/android/campung 폴더 선택
4. Gradle 동기화 대기
5. 상단의 Run 버튼 클릭 또는 Shift + F10
```

### 2. 명령줄에서 빌드
```bash
cd android/campung

# Windows
./gradlew assembleDebug

# macOS/Linux  
./gradlew assembleDebug
```

### 3. APK 설치
```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 기기에 설치
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📋 주요 기능

### 핵심 기능
- 📍 **위치 기반 지도** - Naver Maps SDK 활용
- 👥 **친구 위치 공유** - 실시간 위치 공유 및 마커 표시
- 📝 **콘텐츠 공유** - 지도 상에서 텍스트/이미지/오디오 공유
- 🏛️ **랜드마크 관리** - POI(관심 지점) 생성, 검색 및 상세 정보 제공
- 🔔 **실시간 알림** - Firebase Cloud Messaging
- 🎵 **오디오 녹음** - 위치 기반 오디오 메시지
- 🌡️ **캠퍼스 현황** - 날씨 및 온도 정보 표시

### 기술 특징
- **MVVM Architecture** - ViewModel, Repository 패턴
- **Jetpack Compose** - 모던 선언형 UI
- **Dependency Injection** - Hilt 사용
- **Local Database** - Room 활용
- **RESTful API** - Retrofit + OkHttp
- **WebSocket** - 실시간 통신
- **Modular Architecture** - 컴포넌트 모듈화

## 🏗️ 프로젝트 구조

```
android/campung/app/src/main/java/com/shinhan/campung/
├── data/                     # 데이터 계층
│   ├── local/               # 로컬 데이터 (Room, DataStore)
│   ├── remote/              # 원격 데이터 (API, WebSocket)
│   ├── model/               # 데이터 모델
│   └── repository/          # Repository 구현체
├── di/                      # Dependency Injection (Hilt)
├── navigation/              # 앱 내 네비게이션
├── presentation/            # UI 계층
│   ├── ui/
│   │   ├── components/      # 재사용 가능한 UI 컴포넌트
│   │   ├── screens/         # 화면별 UI
│   │   └── map/            # 지도 관련 UI
│   └── viewmodel/          # ViewModel 클래스들
└── util/                   # 유틸리티 클래스
```

## 🔐 권한 설정

앱에서 사용하는 주요 권한:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 🐛 문제 해결

### 일반적인 문제
1. **Gradle 동기화 실패**
   ```bash
   ./gradlew clean
   ./gradlew build --refresh-dependencies
   ```

2. **Naver Maps API 오류**
   - `local.properties`의 `NAVER_MAP_CLIENT_ID` 확인
   - 네이버 클라우드 플랫폼에서 API 키 발급

3. **Firebase 연동 오류**
   - `google-services.json` 파일 확인
   - Firebase 프로젝트 설정 검증

4. **빌드 오류 시**
   ```bash
   # 캐시 정리
   ./gradlew clean
   
   # Android Studio 캐시 정리
   File > Invalidate Caches and Restart
   ```

## 📞 지원

### API 문서
```
https://campung.my/swagger-ui/index.html
```

### 개발 환경 지원
- Android API Level 26-36 지원
- Kotlin 2.0+ 호환
- Jetpack Compose 안정 버전 사용

---

**참고**: 이 프로젝트는 캠퍼스 내 위치 기반 소셜 네트워킹을 목표로 하는 Android 네이티브 앱입니다.