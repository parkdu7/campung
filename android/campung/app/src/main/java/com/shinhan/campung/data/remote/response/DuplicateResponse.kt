package com.shinhan.campung.data.remote.response

data class DuplicateResponse(
    val success: Boolean,
    val message: String,
    val available: Boolean
)