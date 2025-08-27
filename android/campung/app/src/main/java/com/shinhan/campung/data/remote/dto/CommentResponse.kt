package com.shinhan.campung.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CommentListResponse(
    val success: Boolean,
    val message: String,
    val data: CommentListData
)

data class CommentListData(
    val comments: List<CommentDto>
)

data class CommentDto(
    val commentId: Long,
    val userId: String,
    val author: AuthorDto,
    val body: String,
    val mediaFiles: List<MediaFileDto>?,
    val createdAt: String,
    val replies: List<ReplyDto>?,
    val replyCount: Int
)

data class ReplyDto(
    val replyId: Long,
    val userId: String,
    val author: AuthorDto,
    val body: String,
    val mediaFiles: List<MediaFileDto>?,
    val createdAt: String
)

data class AuthorDto(
    val nickname: String,
    val profileImageUrl: String?,
    val isAnonymous: Boolean
)

data class MediaFileDto(
    val fileId: Long,
    val fileType: String,
    val fileUrl: String,
    val thumbnailUrl: String?,
    val duration: Int? = null
)

data class LikeResponse(
    val success: Boolean,
    val message: String,
    val data: LikeData
)

data class LikeData(
    val totalLikes: Int,
    val liked: Boolean
)