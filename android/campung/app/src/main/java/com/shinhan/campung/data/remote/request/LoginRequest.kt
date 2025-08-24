package com.shinhan.campung.data.remote.request

data class LoginRequest(
    val userId: String,
    val password: String,
    val fcmToken: String? = null
)