package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.location.LocationTracker
import com.shinhan.campung.data.remote.response.PostType
import com.shinhan.campung.data.repository.ContentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WritePostViewModel @Inject constructor(
    private val contentsRepository: ContentsRepository,
    private val locationTracker: LocationTracker,   // ✅ 현재 위치 주입
) : ViewModel() {

    data class UiState(val isLoading: Boolean = false)

    sealed class Event {
        data class Success(val contentId: Long, val message: String) : Event()
        data class Error(val message: String) : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    /** 화면의 board title -> 서버 PostType 매핑 */
    private fun mapBoardToPostType(boardTitle: String?): PostType = when (boardTitle) {
        "장터게시판" -> PostType.MARKET
        "홍보게시판" -> PostType.NOTICE
        "정보게시판" -> PostType.INFO
        "자유게시판" -> PostType.FREE
        else -> PostType.FREE
    }

    // 좌표 해석: null이면 현재 위치 시도 → 실패 시 (0.0, 0.0)
    private suspend fun resolveCoords(
        lat: Double?, lng: Double?
    ): Pair<Double, Double> {
        if (lat != null && lng != null) return lat to lng
        return try {
            val loc = locationTracker.getCurrentLocation() // Location? 반환 가정
            if (loc != null) loc.latitude to loc.longitude else 0.0 to 0.0
        } catch (_: Exception) {
            0.0 to 0.0
        }
    }

    /**
     * 글 등록
     * - emotionTag, files: nullable
     * - isRealName = true면 isAnonymous=false 로 변환
     * - latitude/longitude가 null이면 현재 위치를 자동 사용
     */
    fun submit(
        boardTitle: String?,
        title: String,
        body: String,
        isRealName: Boolean,
        emotionTag: String? = null,
        files: List<String>? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val postType = mapBoardToPostType(boardTitle)
        val isAnonymous = !isRealName

        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)

            val (lat, lng) = resolveCoords(latitude, longitude) // ✅ 좌표 확정

            val result = contentsRepository.createContentFormUrlEncoded(
                title = title,
                body = body,
                latitude = lat,
                longitude = lng,
                postType = postType,
                isAnonymous = isAnonymous,
                contentScope = "MAP",
                emotionTag = emotionTag,
                files = files
            )

            _uiState.value = UiState(isLoading = false)

            result.onSuccess { res ->
                _events.emit(Event.Success(res.contentId, res.message))
            }.onFailure { e ->
                _events.emit(Event.Error(e.message ?: "등록에 실패했어요."))
            }
        }
    }
}
