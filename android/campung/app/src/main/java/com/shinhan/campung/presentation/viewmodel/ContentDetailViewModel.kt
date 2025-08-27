package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.repository.ContentRepository
import com.shinhan.campung.presentation.ui.screens.contentdetail.ContentDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContentDetailViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentDetailUiState())
    val uiState: StateFlow<ContentDetailUiState> = _uiState.asStateFlow()

    fun loadContent(contentId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val content = contentRepository.getContent(contentId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    content = content,
                    isLiked = content.reactions.isLiked,
                    likeCount = content.reactions.likes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "컨텐츠를 불러올 수 없습니다: ${e.message}"
                )
            }
        }
    }

    fun loadComments(contentId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCommentLoading = true)
            
            try {
                val comments = contentRepository.getComments(contentId)
                _uiState.value = _uiState.value.copy(
                    comments = comments,
                    isCommentLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCommentLoading = false,
                    error = "댓글을 불러올 수 없습니다: ${e.message}"
                )
            }
        }
    }

    fun toggleLike(contentId: Long) {
        viewModelScope.launch {
            try {
                val result = contentRepository.toggleLike(contentId)
                _uiState.value = _uiState.value.copy(
                    isLiked = result.isLiked,
                    likeCount = result.totalLikes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "좋아요 처리 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun updateCommentText(text: String) {
        _uiState.value = _uiState.value.copy(commentText = text)
    }

    fun postComment(contentId: Long, body: String) {
        if (body.isBlank()) return
        
        viewModelScope.launch {
            // 댓글 전송 시작하면 즉시 텍스트 초기화
            _uiState.value = _uiState.value.copy(commentText = "")
            
            try {
                contentRepository.postComment(contentId, body.trim(), false)
                // 댓글 목록 새로고침
                loadComments(contentId)
            } catch (e: Exception) {
                // 에러 발생시 텍스트 복원 및 에러 메시지 표시
                _uiState.value = _uiState.value.copy(
                    commentText = body,
                    error = "댓글 작성 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}