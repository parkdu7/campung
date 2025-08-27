package com.shinhan.campung.data.remote.request

import com.google.gson.annotations.SerializedName

data class LocationShareRequestRequest(
    @SerializedName("friendUserIds")
    val friendUserIds: List<String>,
    
    @SerializedName("purpose")
    val purpose: String
)