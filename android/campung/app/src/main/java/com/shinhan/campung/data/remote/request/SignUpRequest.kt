package com.shinhan.campung.data.remote.request

data class SignUpRequest(
    val userId: String,
    val password: String,
    val nickname: String
)