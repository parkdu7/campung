# 캠퍼스 감정 API 명세서

본 문서는 캠퍼스 감정(날씨, 온도, 점수)과 관련된 API의 명세를 정의합니다.

## 1. 감정 통계 전체 조회

현재 캠퍼스의 감정 관련 모든 통계(평균 점수, 날씨, 온도)를 조회합니다.

- **URL:** `/api/emotion/statistics`
- **HTTP Method:** `GET`
- **Request Parameters:** 없음
- **Success Response (200 OK):**
    ```json
    {
      "success": true,
      "data": {
        "averageScores": {
          "우울함": 45.2,
          "밝음": 78.9,
          "신남": 65.3,
          "화남": 32.1,
          "슬픔": 38.7,
          "흥분된": 58.4
        },
        "emotionWeather": "구름 조금",
        "emotionTemperature": 24.3,
        "lastUpdated": "2025-01-27T15:00:00"
      }
    }
    ```

---

## 2. 감정 날씨/온도 조회

현재 캠퍼스의 감정 날씨와 온도 정보만 간단히 조회합니다.

- **URL:** `/api/emotion/weather`
- **HTTP Method:** `GET`
- **Request Parameters:** 없음
- **Success Response (200 OK):**
    ```json
    {
      "success": true,
      "data": {
        "emotionWeather": "맑음",
        "emotionTemperature": 28.5,
        "lastUpdated": "2025-01-27T15:00:00"
      }
    }
    ```

---

## 3. 감정 점수 조회

현재 캠퍼스의 감정별 평균 점수만 조회합니다.

- **URL:** `/api/emotion/scores`
- **HTTP Method:** `GET`
- **Request Parameters:** 없음
- **Success Response (200 OK):**
    ```json
    {
      "success": true,
      "data": {
        "averageScores": {
          "우울함": 45.2,
          "밝음": 78.9,
          "신남": 65.3,
          "화남": 32.1,
          "슬픔": 38.7,
          "흥분된": 58.4
        },
        "lastUpdated": "2025-01-27T15:00:00"
      }
    }
    ```

---

## 4. 감정 분석 수동 실행 (관리용)

지난 1시간 동안의 게시글에 대한 감정 분석을 수동으로 실행하고, 그 결과를 반영한 최신 감정 통계를 반환합니다.

- **URL:** `/api/emotion/analyze`
- **HTTP Method:** `POST`
- **Request Body:** 없음
- **Success Response (200 OK):**
    - 감정 분석이 완료된 후의 **감정 통계 전체 조회** 결과와 동일한 형식의 데이터를 반환합니다.
    ```json
    {
      "success": true,
      "data": {
        "averageScores": { ... },
        "emotionWeather": "...",
        "emotionTemperature": ...,
        "lastUpdated": "..."
      }
    }
    ```

---

## 5. 일일 감정 데이터 초기화 (관리용)

그날 05시부터 누적된 모든 감정 데이터를 초기화합니다. 날씨는 '흐림', 온도는 '20.0'으로 설정됩니다.

- **URL:** `/api/emotion/reset`
- **HTTP Method:** `POST`
- **Request Body:** 없음
- **Success Response (200 OK):**
    ```json
    {
      "success": true,
      "message": "감정 데이터가 초기화되었습니다."
    }
    ```
