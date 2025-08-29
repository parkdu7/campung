package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.response.RecordUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface RecordingApiService {

    /**
     * POST /records
     * - Multipart(Form-Data) 업로드
     * - audioFile: 파일 파트
     * - latitude / longitude: 텍스트 파트(숫자이지만 폼데이터 특성상 text/plain)
     */
    @Multipart
    @POST("records")
    suspend fun uploadRecord(
        @Part audioFile: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody
    ): RecordUploadResponse

    /**
     * DELETE /records/{recordId}
     * - 음성 녹음 삭제
     */
    @DELETE("records/{recordId}")
    suspend fun deleteRecord(
        @Path("recordId") recordId: Long
    )
}