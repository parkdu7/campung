package com.shinhan.campung.data.repository

import com.shinhan.campung.data.remote.api.RecordingApiService
import com.shinhan.campung.data.remote.response.RecordUploadResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(
    private val api: RecordingApiService
) : RecordingRepository {

    override suspend fun uploadRecord(
        audioFile: File,
        latitude: Double,
        longitude: Double
    ): RecordUploadResponse {
        // 파일 파트 (name = "audioFile" 반드시 서버 스펙과 동일)
        val audioPart = MultipartBody.Part.createFormData(
            name = "audioFile",
            filename = audioFile.name,
            body = audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
        )

        // 숫자 메타데이터는 multipart text 파트로 전송
        val textMedia = "text/plain".toMediaType()
        val latPart: RequestBody = latitude.toString().toRequestBody(textMedia)
        val lngPart: RequestBody = longitude.toString().toRequestBody(textMedia)

        return api.uploadRecord(
            audioFile = audioPart,
            latitude = latPart,
            longitude = lngPart
        )
    }
}