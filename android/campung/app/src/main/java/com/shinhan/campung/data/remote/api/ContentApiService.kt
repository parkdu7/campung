package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.response.ContentCreateResponse
import okhttp3.RequestBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ContentsApiService {

    /**
     * 1) application/x-www-form-urlencoded
     * - files는 같은 키를 반복 전송 (files=a&files=b)
     * - 서버가 files[] 형태를 요구하면 아래 @Field("files[]")로 키만 바꾸면 됨.
     */
    @FormUrlEncoded
    @POST("contents")
    suspend fun createContentFormUrlEncoded(
        @Field("title") title: String,
        @Field("body") body: String,
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Field("contentScope") contentScope: String,   // 기본 "MAP"을 Repository에서 제공
        @Field("postType") postType: String,          // PostType.name
        @Field("emotionTag") emotionTag: String?,     // nullable
        @Field("isAnonymous") isAnonymous: Boolean,
        @Field("files") files: List<String>?          // 반복 필드 전송
        // @Field("files[]") files: List<String>?     // 서버가 [] 표기를 요구한다면 이걸로 변경
    ): ContentCreateResponse

    /**
     * 2) multipart/form-data
     * - 전부 텍스트 파트로 전송 (파일 바이너리는 아님)
     * - 같은 키(files)를 List로 여러 번 보냄
     */
    @Multipart
    @POST("contents")
    suspend fun createContentMultipart(
        @Part("title") title: RequestBody,
        @Part("body") body: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("contentScope") contentScope: RequestBody,
        @Part("postType") postType: RequestBody,
        @Part("emotionTag") emotionTag: RequestBody?,   // nullable
        @Part("isAnonymous") isAnonymous: RequestBody,
        @Part("files") files: List<RequestBody>?        // 반복 파트
        // @Part("files[]") files: List<RequestBody>?   // [] 표기 필요 시
    ): ContentCreateResponse
}