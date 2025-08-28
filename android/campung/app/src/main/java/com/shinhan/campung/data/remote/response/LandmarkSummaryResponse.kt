package com.shinhan.campung.data.remote.response

data class LandmarkSummaryResponse(
    val landmarkId: Long,
    val summary: String,
    val generatedAt: String,
    val postCount: Int,
    val keywords: List<String>
)