package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.dto.CommentListResponse
import com.shinhan.campung.data.remote.dto.CommentRequest
import com.shinhan.campung.data.remote.dto.CommentResponse
import com.shinhan.campung.data.remote.dto.LikeResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ContentApiService {

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