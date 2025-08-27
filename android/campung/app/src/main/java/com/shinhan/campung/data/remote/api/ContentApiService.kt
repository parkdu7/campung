package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.response.ContentCreateResponse
import okhttp3.MultipartBody
import com.shinhan.campung.data.remote.dto.CommentListResponse
import com.shinhan.campung.data.remote.dto.CommentRequest
import com.shinhan.campung.data.remote.dto.CommentResponse
import com.shinhan.campung.data.remote.dto.LikeResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ContentApiService {

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
    // 댓글 목록 조회
    @GET("/api/contents/{contentId}/comments")
    suspend fun getComments(
        @Path("contentId") contentId: Long
    ): Response<CommentListResponse>

    // 댓글 작성
    @POST("/api/contents/{contentId}/comments")
    suspend fun postComment(
        @Path("contentId") contentId: Long,
        @Query("body") body: String,
        @Query("isAnonymous") isAnonymous: Boolean
    ): Response<CommentResponse>

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

    // 대댓글 작성
    @Multipart
    @POST("/api/contents/{contentId}/comments/{commentId}/replies")
    suspend fun postReply(
        @Path("contentId") contentId: Long,
        @Path("commentId") commentId: Long,
        @Part("body") body: RequestBody,
        @Part("isAnonymous") isAnonymous: RequestBody,
        @Part("parentCommentId") parentCommentId: RequestBody
    ): Response<CommentResponse>

    // 좋아요 토글
    @POST("/api/contents/{contentId}/like")
    suspend fun toggleLike(
        @Path("contentId") contentId: Long
    ): Response<LikeResponse>
}