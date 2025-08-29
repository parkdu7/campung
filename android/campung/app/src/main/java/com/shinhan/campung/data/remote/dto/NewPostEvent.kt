package com.shinhan.campung.data.remote.dto

data class NewPostEvent(
    val postId: String,
    val lat: Double,
    val lon: Double,
    val timestamp: Long,
    val userId: String? = null  // 작성자 ID 추가
)