package com.shinhan.campung.data.repository

import com.shinhan.campung.data.remote.api.ContentsApiService
import com.shinhan.campung.data.remote.response.ContentCreateResponse
import com.shinhan.campung.data.remote.response.PostType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentsRepository @Inject constructor(
    private val api: ContentsApiService
) {
    private val TEXT = "text/plain".toMediaType()

    // ---- helpers (텍스트 → RequestBody) ----
    private fun String.asBody(): RequestBody = this.toRequestBody(TEXT)
    private fun Double.asBody(): RequestBody = this.toString().toRequestBody(TEXT)
    private fun Boolean.asBody(): RequestBody = this.toString().toRequestBody(TEXT)

    /**
     * A. x-www-form-urlencoded 방식
     * - 가장 간단. 서버가 폼 URL 인코딩 기대할 때 사용.
     */
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
        useBracketForFiles: Boolean = false // 서버가 files[] 요구 시 true로 하고 ApiService도 키 변경 필요
    ): Result<ContentCreateResponse> = runCatching {
        api.createContentFormUrlEncoded(
            title = title,
            body = body,
            latitude = latitude,
            longitude = longitude,
            contentScope = contentScope,
            postType = postType.name,
            emotionTag = emotionTag,
            isAnonymous = isAnonymous,
            files = files
        )
    }

    /**
     * B. multipart/form-data 방식
     * - 추후 바이너리 파일 업로드와 혼용하기 쉬움(현재는 모두 텍스트 파트).
     */
    suspend fun createContentMultipart(
        title: String,
        body: String,
        latitude: Double,
        longitude: Double,
        postType: PostType,
        isAnonymous: Boolean,
        contentScope: String = "MAP",
        emotionTag: String? = null,
        files: List<String>? = null,
        useBracketForFiles: Boolean = false // 서버가 files[] 요구 시 ApiService도 키 변경 필요
    ): Result<ContentCreateResponse> = runCatching {
        val fileParts: List<RequestBody>? = files?.map { it.asBody() }

        api.createContentMultipart(
            title = title.asBody(),
            body = body.asBody(),
            latitude = latitude.asBody(),
            longitude = longitude.asBody(),
            contentScope = contentScope.asBody(),
            postType = postType.name.asBody(),
            emotionTag = emotionTag?.asBody(),
            isAnonymous = isAnonymous.asBody(),
            files = fileParts
        )
    }
}
