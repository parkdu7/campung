package com.shinhan.campung.data.remote.response

data class SignUpResponse(
    val success: Boolean,
    val message: String,
    val accessToken: String
)