package com.example.campung.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CampusDateUtil {
    
    private static final int CAMPUS_DAY_START_HOUR = 5; // 05시 시작
    
    /**
     * 현재 시각 기준으로 캠퍼스 날짜를 계산합니다.
     * 05:00 ~ 다음날 04:59:59 를 하나의 캠퍼스 날짜로 봅니다.
     * 
     * 예시:
     * - 2024-08-29 23:30 → 2024-08-29 (캠퍼스 날짜)
     * - 2024-08-30 02:30 → 2024-08-29 (캠퍼스 날짜)
     * - 2024-08-30 05:00 → 2024-08-30 (캠퍼스 날짜)
     */
    public static LocalDate getCurrentCampusDate() {
        LocalDateTime now = LocalDateTime.now();
        return getCampusDate(now);
    }
    
    /**
     * 주어진 시각의 캠퍼스 날짜를 계산합니다.
     */
    public static LocalDate getCampusDate(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        LocalDate date = dateTime.toLocalDate();
        
        // 05:00 이전이면 전날로 간주
        if (time.isBefore(LocalTime.of(CAMPUS_DAY_START_HOUR, 0))) {
            return date.minusDays(1);
        } else {
            return date;
        }
    }
    
    /**
     * 캠퍼스 날짜에 해당하는 시작 시각을 반환합니다.
     * 예: 2024-08-29 → 2024-08-29 05:00:00
     */
    public static LocalDateTime getCampusDateStartTime(LocalDate campusDate) {
        return campusDate.atTime(CAMPUS_DAY_START_HOUR, 0, 0);
    }
    
    /**
     * 캠퍼스 날짜에 해당하는 종료 시각을 반환합니다.
     * 예: 2024-08-29 → 2024-08-30 04:59:59
     */
    public static LocalDateTime getCampusDateEndTime(LocalDate campusDate) {
        return campusDate.plusDays(1).atTime(CAMPUS_DAY_START_HOUR - 1, 59, 59);
    }
    
    /**
     * 캠퍼스 날짜 문자열(yyyy-MM-dd)을 파싱합니다.
     * null이거나 빈 문자열이면 현재 캠퍼스 날짜를 반환합니다.
     */
    public static LocalDate parseCampusDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return getCurrentCampusDate();
        }
        
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            // 파싱 실패 시 현재 캠퍼스 날짜 반환
            return getCurrentCampusDate();
        }
    }
    
    /**
     * 디버그용 메서드: 캠퍼스 날짜 정보를 출력합니다.
     */
    public static String debugCampusDate(LocalDateTime dateTime) {
        LocalDate campusDate = getCampusDate(dateTime);
        LocalDateTime startTime = getCampusDateStartTime(campusDate);
        LocalDateTime endTime = getCampusDateEndTime(campusDate);
        
        return String.format(
            "실제시각: %s → 캠퍼스날짜: %s (범위: %s ~ %s)",
            dateTime, campusDate, startTime, endTime
        );
    }
}