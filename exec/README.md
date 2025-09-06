# 🏫 Campung Fullstack

> **차세대 AI 기반 캠퍼스 커뮤니티 플랫폼**  
> 실시간 감정 분석, 위치 공유, AI 분위기 요약을 통한 스마트 캠퍼스 라이프

## 📋 목차
- [프로젝트 소개](#-프로젝트-소개)  
- [주요 기능](#-주요-기능)  
- [기술 스택](#-기술-스택)  
- [아키텍처](#-아키텍처)  
- [백엔드 (Spring Boot)](#-백엔드-spring-boot)  
- [안드로이드 (Kotlin Compose)](#-안드로이드-kotlin-compose)  
- [API 문서](#-api-문서)  
- [설치 및 실행](#-설치-및-실행)  
- [환경 변수](#-환경-변수)  
- [문제 해결](#-문제-해결)

---

## 🎯 프로젝트 소개
**Campung**은 캠퍼스 내 학생들의 일상과 감정을 AI로 분석하여, 실시간 캠퍼스 분위기를 제공하는 차세대 커뮤니티 플랫폼입니다.  

- **🤖 AI 기반 인사이트**: GPT-5 감정 분석 및 분위기 요약  
- **📍 위치 기반 서비스**: Geohash 기반 정밀 위치 처리  
- **⚡ 실시간 경험**: WebSocket + FCM 알림  
- **🌡️ 캠퍼스 온도**: 활동량·감정을 바탕으로 지표화  

---

## 🚀 주요 기능
### 공통
- 실시간 캠퍼스 온도 지표
- AI 기반 건물/랜드마크 분위기 요약
- 실시간 위치 공유 및 친구 관계 관리
- DB+FCM 기반 이중 알림

### 백엔드 특화
- Redis 기반 HOT 게시글 & 캐싱
- GPT-5-mini 기반 비용 최적화 AI 분석
- Docker Compose + Gradle 배포

### 안드로이드 특화
- Naver Maps SDK 기반 지도 & 위치 공유
- Jetpack Compose 기반 모던 UI
- 오디오 녹음/재생, 실시간 WebSocket 반영
- Firebase 연동 푸시 알림

---

## 🛠 기술 스택
### Backend
- **Framework**: Spring Boot 3.x, Spring Security  
- **DB**: MariaDB, Redis 7.x  
- **AI**: OpenAI GPT-5 API  
- **Infra**: AWS S3, Firebase FCM  
- **Etc.**: JPA/QueryDSL, Gradle, Docker Compose  

### Android
- **언어/SDK**: Kotlin 2.0.21, Compose BOM 2024.12, SDK 26~36  
- **아키텍처**: MVVM + Hilt DI + Room  
- **네트워크**: Retrofit, OkHttp, WebSocket  
- **지도/위치**: Naver Maps SDK, Google Play Services Location  
- **멀티미디어**: ExoPlayer, Coil, Lottie  
- **Firebase**: Messaging (FCM), google-services.json  

---

## 🏗 아키텍처
```
📦 Campung Architecture

┌──────────────┐    ┌──────────────┐
│ Android App  │───▶│ Spring Boot  │───▶ MariaDB
│ (Compose UI) │    │   Backend    │───▶ Redis
└──────────────┘    └──────────────┘
        │                    │
        ▼                    ▼
   Firebase FCM ◀────────▶ AWS S3
        │
   OpenAI GPT-5
```

---

## 🖥 백엔드 (Spring Boot)
- GPT-5 배치 감정 분석 (30개 청크 최적화)  
- Geohash 기반 위치 인덱싱 (8자리 ≈ 40m)  
- 동적 캠퍼스 온도 알고리즘  
- WebSocket 지역별 브로드캐스팅  
- 패키지 구조: Content / Landmark / Emotion / User&Friendship / LocationShare / Notification  

설치·실행, `docker-compose.yml`, `env.properties` 예시는 Backend README와 동일.

---

## 📱 안드로이드 (Kotlin Compose)
- 지도: Naver Maps + Compose  
- 친구 위치 공유: 실시간 마커  
- 콘텐츠 공유: 텍스트·이미지·오디오  
- 랜드마크 관리 & 캠퍼스 현황 표시  
- Jetpack Compose + Hilt + Room 기반 구조  

프로젝트 구조:
```
android/campung/app/src/main/java/com/shinhan/campung/
├── data/ (local, remote, model, repository)
├── di/ (Hilt)
├── navigation/
├── presentation/ (UI, screens, viewmodel)
└── util/
```

빌드 & 실행법, `local.properties`, `google-services.json` 설정은 Android README와 동일.

---

## 📖 API 문서
- Swagger UI:  
  ```
  http://localhost:8080/swagger-ui/index.html
  https://campung.my/swagger-ui/index.html
  ```

---

## ⚙️ 설치 및 실행
1. **Backend**
   - `docker-compose up -d` (MariaDB, Redis, phpMyAdmin)  
   - `./gradlew bootRun`  

2. **Android**
   - Android Studio에서 `android/campung` 열기  
   - `local.properties` & `google-services.json` 설정  
   - `./gradlew assembleDebug` 후 설치  

---

## 🔐 환경 변수
- `env.properties` (Backend): DB/Redis/AWS/OpenAI/Firebase 키  
- `local.properties` (Android): SDK 경로, NAVER_MAP_CLIENT_ID  
- Firebase `google-services.json`  

---

## 🐛 문제 해결
### Backend
- DB/Redis 포트 충돌 → docker-compose.yml 수정  
- OpenAI API Key 미설정 시 감정 분석 실패  

### Android
- Gradle 동기화 실패 → `./gradlew clean build --refresh-dependencies`  
- Naver Maps API 오류 → `NAVER_MAP_CLIENT_ID` 확인  
- Firebase 오류 → `google-services.json` 확인  
