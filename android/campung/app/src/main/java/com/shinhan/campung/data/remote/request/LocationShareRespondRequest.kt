package com.shinhan.campung.data.remote.request

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class LocationShareRespondRequest(
    @SerializedName("action")
    val action: String, // "accept" or "reject"
    
    @SerializedName("latitude")
    val latitude: BigDecimal? = null, // 수락 시에만 포함
    
    @SerializedName("longitude")
    val longitude: BigDecimal? = null // 수락 시에만 포함
)