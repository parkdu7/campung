package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName

data class NotificationListResponse(
    @SerializedName("success")
    val success: Boolean = true,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("data")
    val data: List<NotificationResponse>? = null
)