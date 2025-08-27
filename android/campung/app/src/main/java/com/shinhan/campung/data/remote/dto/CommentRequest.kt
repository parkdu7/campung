package com.shinhan.campung.data.remote.dto

data class CommentRequest(
    val body: String,
    val isAnonymous: Boolean,
    val parentCommentId: Long? = null
)

data class CommentResponse(
    val success: Boolean,
    val message: String,
    val commentId: Long
)