package com.shinhan.campung.data.remote.dto

data class NewPostEvent(
    val postId: String,
    val lat: Double,
    val lon: Double,
    val timestamp: Long
)