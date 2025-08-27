package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName

data class RecordUploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("recordId") val recordId: Long
)