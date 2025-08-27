package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("notificationId")
    val notificationId: Long,
    
    @SerializedName("type")
    val type: String, // "normal", "friendRequest", "locationShareRequest"
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: String? = null, // JSON 형태의 추가 데이터
    
    @SerializedName("isRead")
    val isRead: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("readAt")
    val readAt: String? = null
)