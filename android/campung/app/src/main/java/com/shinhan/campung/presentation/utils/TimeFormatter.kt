package com.shinhan.campung.presentation.utils

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimeFormatter {
    fun formatRelativeTime(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val duration = Duration.between(dateTime, now)
        
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