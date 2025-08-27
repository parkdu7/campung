package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.response.ContentCreateResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ContentsApiService {

    @FormUrlEncoded
    @POST("contents")
    suspend fun createContentFormUrlEncoded(
        @Field("title") title: String,
        @Field("body") body: String,
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Field("contentScope") contentScope: String,
        @Field("postType") postType: String,
        @Field("emotionTag") emotionTag: String?,
        @Field("isAnonymous") isAnonymous: Boolean,
        @Field("files") files: List<String>?,   // URL/키를 문자열로 보낼 때만 사용
    ): ContentCreateResponse

    @Multipart
    @POST("contents")
    suspend fun createContentMultipart(
        @Part("title") title: RequestBody,
        @Part("body") body: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("contentScope") contentScope: RequestBody,
        @Part("postType") postType: RequestBody,
        @Part("emotionTag") emotionTag: RequestBody?,
        @Part("isAnonymous") isAnonymous: RequestBody,
        @Part files: List<MultipartBody.Part>?,    // ✅ 파일은 Part로!
    ): ContentCreateResponse
}