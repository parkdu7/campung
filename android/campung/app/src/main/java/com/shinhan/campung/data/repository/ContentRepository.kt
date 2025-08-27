package com.shinhan.campung.data.repository

import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.mapper.CommentMapper
import com.shinhan.campung.data.model.Comment
import com.shinhan.campung.data.model.LikeResponse
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.remote.api.ContentsApiService
import kotlinx.coroutines.flow.first
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

interface ContentRepository {
    suspend fun getContent(contentId: Long): MapContent
    suspend fun getComments(contentId: Long): List<Comment>
    suspend fun getLikeStatus(contentId: Long): LikeResponse
    suspend fun toggleLike(contentId: Long): LikeResponse
    suspend fun postComment(contentId: Long, body: String, isAnonymous: Boolean)
    suspend fun postReply(contentId: Long, commentId: Long, body: String, isAnonymous: Boolean)
}

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val mapContentRepository: MapContentRepository,
    private val contentApiService: ContentsApiService,
    private val commentMapper: CommentMapper,
    private val authDataStore: AuthDataStore
) : ContentRepository {

    private suspend fun getAuthHeader(): String {
        val token = authDataStore.tokenFlow.first()
        return "Bearer $token"
    }

    override suspend fun getContent(contentId: Long): MapContent {
        // 기존 MapContentRepository 사용
        val result = mapContentRepository.getContentById(contentId)
        return result.getOrThrow()
    }

    override suspend fun getComments(contentId: Long): List<Comment> {
        try {
            val response = contentApiService.getComments(
                contentId = contentId
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val commentDtos = response.body()?.data?.comments ?: emptyList()
                return commentDtos.map { commentMapper.toDomain(it) }
            } else {
                throw Exception("댓글 조회 실패: ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("댓글 조회 중 오류 발생: ${e.message}")
        }
    }

    override suspend fun getLikeStatus(contentId: Long): LikeResponse {
        try {
            val content = getContent(contentId)
            return LikeResponse(
                isLiked = content.reactions.isLiked, // ✅ 실제 사용자 좋아요 상태 사용
                totalLikes = content.reactions.likes
            )
        } catch (e: Exception) {
            return LikeResponse(isLiked = false, totalLikes = 0)
        }
    }

    override suspend fun toggleLike(contentId: Long): LikeResponse {
        try {
            val response = contentApiService.toggleLike(
                contentId = contentId
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val likeData = response.body()?.data
                return LikeResponse(
                    isLiked = likeData?.liked ?: false,
                    totalLikes = likeData?.totalLikes ?: 0
                )
            } else {
                throw Exception("좋아요 처리 실패: ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("좋아요 처리 중 오류 발생: ${e.message}")
        }
    }

    override suspend fun postComment(contentId: Long, body: String, isAnonymous: Boolean) {
        try {
            val response = contentApiService.postComment(
                contentId = contentId,
                body = body.toRequestBody(),
                isAnonymous = isAnonymous.toString().toRequestBody(),
                parentCommentId = "".toRequestBody() // 일반 댓글은 빈 문자열
            )
            
            if (!response.isSuccessful || response.body()?.success != true) {
                throw Exception("댓글 작성 실패: ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("댓글 작성 중 오류 발생: ${e.message}")
        }
    }
    
    override suspend fun postReply(contentId: Long, commentId: Long, body: String, isAnonymous: Boolean) {
        try {
            val response = contentApiService.postReply(
                contentId = contentId,
                commentId = commentId,
                body = body.toRequestBody(),
                isAnonymous = isAnonymous.toString().toRequestBody(),
                parentCommentId = commentId.toString().toRequestBody()
            )
            
            if (!response.isSuccessful || response.body()?.success != true) {
                throw Exception("대댓글 작성 실패: ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("대댓글 작성 중 오류 발생: ${e.message}")
        }
    }
}