package com.shinhan.campung.data.remote.response

enum class PostType { NOTICE, INFO, MARKET, FREE }

data class ContentCreateResponse(
    val success: Boolean,
    val message: String,
    val contentId: Long
)