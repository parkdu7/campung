package com.shinhan.campung.presentation.ui.screens.contentdetail

import com.shinhan.campung.data.model.Comment
import com.shinhan.campung.data.model.MapContent

data class ContentDetailUiState(
    val isLoading: Boolean = false,
    val content: MapContent? = null,
    val comments: List<Comment> = emptyList(),
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val commentText: String = "",
    val error: String? = null,
    val isCommentLoading: Boolean = false,
    
    // 대댓글 모드 상태
    val selectedCommentId: Long? = null,
    val selectedCommentAuthor: String? = null,
) {
    val hasContent: Boolean get() = content != null
    val commentCount: Int get() = comments.size
    
    // 대댓글 모드 여부
    val isReplyMode: Boolean get() = selectedCommentId != null
}