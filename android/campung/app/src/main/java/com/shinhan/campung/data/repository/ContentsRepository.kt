package com.shinhan.campung.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.shinhan.campung.data.remote.api.ContentsApiService
import com.shinhan.campung.data.remote.response.ContentCreateResponse
import com.shinhan.campung.data.remote.response.PostType
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentsRepository @Inject constructor(
    private val api: ContentsApiService,
    @ApplicationContext private val context: Context,        // ✅ ContentResolver 필요
) {
    private val TEXT = "text/plain".toMediaType()

    private fun String.asText(): RequestBody = this.toRequestBody(TEXT)
    private fun Double.asText(): RequestBody = this.toString().toRequestBody(TEXT)
    private fun Boolean.asText(): RequestBody = this.toString().toRequestBody(TEXT)

    // content:// → temp file → RequestBody → Multipart Part
    private fun uriToPart(
        uri: Uri,
        partName: String = "files"           // 서버가 files[] 요구하면 "files[]"
    ): MultipartBody.Part {
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: "application/octet-stream"
        val fileName = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
            } ?: "upload"

        // 임시 파일로 복사 (대용량 대비 스트리밍도 가능하지만 구현 간단화를 위해)
        val temp = File.createTempFile("upload_", "_tmp", context.cacheDir)
        cr.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { out -> input.copyTo(out) }
        }

        val body = temp.asRequestBody(mime.toMediaType())
        return MultipartBody.Part.createFormData(partName, fileName, body)
    }

    // 통합된 컨텐츠 생성 함수 - 항상 multipart/form-data 사용
    suspend fun createContent(
        title: String,
        body: String,
        latitude: Double,
        longitude: Double,
        postType: PostType,
        isAnonymous: Boolean,
        contentScope: String = "MAP",
        emotionTag: String? = null,
        fileUris: List<Uri>? = null,
        useBracketForFiles: Boolean = false,   // 서버가 files[] 요구 시 true
    ): Result<ContentCreateResponse> = runCatching {
        val partName = if (useBracketForFiles) "files[]" else "files"
        val fileParts: List<MultipartBody.Part>? = fileUris?.takeIf { it.isNotEmpty() }?.map { uriToPart(it, partName) }

        api.createContentMultipart(
            title = title.asText(),
            body = body.asText(),
            latitude = latitude.asText(),
            longitude = longitude.asText(),
            contentScope = contentScope.asText(),
            postType = postType.name.asText(),
            emotionTag = emotionTag?.asText(),
            isAnonymous = isAnonymous.asText(),
            files = fileParts  // 파일이 없으면 null, 있으면 multipart로 전송
        )
    }

    // 기존 함수들은 새로운 통합 함수로 위임 (하위 호환성)
    @Deprecated("Use createContent instead", ReplaceWith("createContent(title, body, latitude, longitude, postType, isAnonymous, contentScope, emotionTag, null)"))
    suspend fun createContentFormUrlEncoded(
        title: String,
        body: String,
        latitude: Double,
        longitude: Double,
        postType: PostType,
        isAnonymous: Boolean,
        contentScope: String = "MAP",
        emotionTag: String? = null,
        files: List<String>? = null,
    ): Result<ContentCreateResponse> = createContent(title, body, latitude, longitude, postType, isAnonymous, contentScope, emotionTag, null)

    @Deprecated("Use createContent instead", ReplaceWith("createContent(title, body, latitude, longitude, postType, isAnonymous, contentScope, emotionTag, fileUris, useBracketForFiles)"))
    suspend fun createContentMultipart(
        title: String,
        body: String,
        latitude: Double,
        longitude: Double,
        postType: PostType,
        isAnonymous: Boolean,
        contentScope: String = "MAP",
        emotionTag: String? = null,
        fileUris: List<Uri>? = null,
        useBracketForFiles: Boolean = false,
    ): Result<ContentCreateResponse> = createContent(title, body, latitude, longitude, postType, isAnonymous, contentScope, emotionTag, fileUris, useBracketForFiles)
}