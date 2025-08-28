package com.shinhan.campung.presentation.utils

import android.util.Log
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormatter {
    fun formatRelativeTime(dateTime: LocalDateTime): String {
        // 한국 시간대 설정
        val koreaZone = ZoneId.of("Asia/Seoul")
        
        // dateTime을 한국 시간대의 ZonedDateTime으로 변환 (이미 한국 시간으로 파싱됨)
        val zonedDateTime = dateTime.atZone(koreaZone)
        
        // 현재 한국 시간
        val now = ZonedDateTime.now(koreaZone)
        
        // 정확한 duration 계산
        val duration = Duration.between(zonedDateTime, now)
        
        // 디버깅용 로그
        Log.d("TimeFormatter", "Original dateTime: $dateTime")
        Log.d("TimeFormatter", "ZonedDateTime: $zonedDateTime")
        Log.d("TimeFormatter", "Current time: $now")
        Log.d("TimeFormatter", "Duration minutes: ${duration.toMinutes()}")
        
        // 미래 시간인 경우 (duration이 음수)
        if (duration.isNegative) {
            Log.d("TimeFormatter", "Duration is negative (${duration.toMinutes()} min), returning '방금 전'")
            return "방금 전"
        }
        
        return when {
            duration.toMinutes() < 1 -> "방금 전"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}분 전"
            duration.toHours() < 24 -> "${duration.toHours()}시간 전"
            duration.toDays() == 1L -> "어제"
            duration.toDays() < 7 -> "${duration.toDays()}일 전"
            else -> dateTime.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
        }
    }
}