package com.shinhan.campung.data.model

import com.google.gson.annotations.SerializedName

data class POIData(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("category")
    val category: String, // "restaurant", "cafe", "convenience", "atm", "hospital", etc.
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("phone")
    val phone: String? = null,
    
    @SerializedName("rating")
    val rating: Float? = null,
    
    @SerializedName("distance")
    val distance: Int? = null, // 미터 단위
    
    @SerializedName("isOpen")
    val isOpen: Boolean? = null,
    
    @SerializedName("openHours")
    val openHours: String? = null,
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null
) {
    val location: LatLngData
        get() = LatLngData(latitude, longitude)
}

data class LatLngData(
    val latitude: Double,
    val longitude: Double
)