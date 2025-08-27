package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName

data class NotificationListResponse(
    @SerializedName("notifications")
    val notifications: List<NotificationResponse>? = null,
    
    @SerializedName("unreadCount")
    val unreadCount: Long = 0,
    
    @SerializedName("totalPages")
    val totalPages: Int = 0,
    
    @SerializedName("totalElements")
    val totalElements: Long = 0,
    
    @SerializedName("currentPage")
    val currentPage: Int = 0,
    
    @SerializedName("size")
    val size: Int = 0
)