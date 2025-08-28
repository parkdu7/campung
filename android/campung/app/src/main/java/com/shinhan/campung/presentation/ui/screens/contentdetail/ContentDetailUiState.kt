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
    
    // 삭제 관련 상태
    val currentUserId: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
) {
    val hasContent: Boolean get() = content != null
    val commentCount: Int get() = content?.commentCount ?: comments.size
    
    // 대댓글 모드 여부
    val isReplyMode: Boolean get() = selectedCommentId != null
    
    // 본인 게시글 여부
    val isMyContent: Boolean get() = currentUserId != null && content?.userId == currentUserId
}