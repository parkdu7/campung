package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName

data class POIResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("data")
    val data: List<POIItem>
)

data class POIItem(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String?,
    
    @SerializedName("category")
    val category: String
)