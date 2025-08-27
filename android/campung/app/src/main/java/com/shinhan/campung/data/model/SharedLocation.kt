package com.shinhan.campung.data.model

import java.time.LocalDateTime

/**
 * 위치 공유 데이터 모델
 */
data class SharedLocation(
    val userName: String,
    val latitude: Double,
    val longitude: Double,
    val displayUntil: LocalDateTime,
    val shareId: String
)