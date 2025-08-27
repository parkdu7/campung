
package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName
import com.shinhan.campung.data.model.ContentData

data class MapContentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: MapContentPage
)

data class MapContentPage(
    @SerializedName("contents") val contents: List<ContentData>,
    @SerializedName("records") val records: List<Any>?,
    @SerializedName("totalCount") val totalCount: Int,
    @SerializedName("hasMore") val hasMore: Boolean,
    // ✅ 서버 공통 날씨/온도 (목록 상단)
    @SerializedName("emotionWeather") val emotionWeather: String?,
    @SerializedName("emotionTemperature") val emotionTemperature: Double?
)
