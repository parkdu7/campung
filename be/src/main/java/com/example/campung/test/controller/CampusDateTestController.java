package com.example.campung.test.controller;

import com.example.campung.global.util.CampusDateUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class CampusDateTestController {
    
    @GetMapping("/campus-date")
    public ResponseEntity<Map<String, Object>> testCampusDate(
            @RequestParam(required = false) String testTime) {
        
        Map<String, Object> result = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        if (testTime != null && !testTime.trim().isEmpty()) {
            try {
                now = LocalDateTime.parse(testTime);
            } catch (Exception e) {
                result.put("error", "잘못된 시간 형식입니다. 예: 2024-08-30T00:27:00");
                return ResponseEntity.badRequest().body(result);
            }
        }
        
        LocalDate currentCampusDate = CampusDateUtil.getCurrentCampusDate();
        LocalDate testCampusDate = CampusDateUtil.getCampusDate(now);
        LocalDateTime startTime = CampusDateUtil.getCampusDateStartTime(testCampusDate);
        LocalDateTime endTime = CampusDateUtil.getCampusDateEndTime(testCampusDate);
        
        result.put("현재시각", LocalDateTime.now().toString());
        result.put("현재_캠퍼스날짜", currentCampusDate.toString());
        result.put("테스트시각", now.toString());
        result.put("테스트_캠퍼스날짜", testCampusDate.toString());
        result.put("캠퍼스날짜_시작시각", startTime.toString());
        result.put("캠퍼스날짜_종료시각", endTime.toString());
        result.put("디버그정보", CampusDateUtil.debugCampusDate(now));
        
        // 예시 케이스들
        Map<String, String> examples = new HashMap<>();
        examples.put("2024-08-29T23:30:00", CampusDateUtil.getCampusDate(LocalDateTime.parse("2024-08-29T23:30:00")).toString());
        examples.put("2024-08-30T00:27:00", CampusDateUtil.getCampusDate(LocalDateTime.parse("2024-08-30T00:27:00")).toString());
        examples.put("2024-08-30T04:59:59", CampusDateUtil.getCampusDate(LocalDateTime.parse("2024-08-30T04:59:59")).toString());
        examples.put("2024-08-30T05:00:00", CampusDateUtil.getCampusDate(LocalDateTime.parse("2024-08-30T05:00:00")).toString());
        
        result.put("예시케이스", examples);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/campus-date-range")
    public ResponseEntity<Map<String, Object>> testCampusDateRange(
            @RequestParam String date) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDate campusDate = LocalDate.parse(date);
            LocalDateTime startTime = CampusDateUtil.getCampusDateStartTime(campusDate);
            LocalDateTime endTime = CampusDateUtil.getCampusDateEndTime(campusDate);
            
            result.put("캠퍼스날짜", campusDate.toString());
            result.put("시작시각", startTime.toString());
            result.put("종료시각", endTime.toString());
            result.put("설명", String.format("%s 캠퍼스 날짜는 %s부터 %s까지입니다", 
                      campusDate, startTime, endTime));
            
        } catch (Exception e) {
            result.put("error", "잘못된 날짜 형식입니다. 예: 2024-08-29");
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok(result);
    }
}