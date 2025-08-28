package com.shinhan.campung.data.repository

import com.shinhan.campung.data.remote.response.RecordUploadResponse
import java.io.File

interface RecordingRepository {
    /**
     * 폼데이터로 오디오 파일 + 위/경도 업로드
     */
    suspend fun uploadRecord(
        audioFile: File,
        latitude: Double,
        longitude: Double
    ): RecordUploadResponse

    /**
     * 음성 녹음 삭제
     */
    suspend fun deleteRecord(recordId: Long)
}