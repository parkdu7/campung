package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.remote.response.FriendResponse
import com.shinhan.campung.data.repository.FriendRepository
import com.shinhan.campung.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    // 친구 목록
    private val _friendsList = MutableStateFlow<List<FriendResponse>>(emptyList())
    val friendsList: StateFlow<List<FriendResponse>> = _friendsList.asStateFlow()

    // 받은 친구 요청
    private val _receivedRequests = MutableStateFlow<List<FriendResponse>>(emptyList())
    val receivedRequests: StateFlow<List<FriendResponse>> = _receivedRequests.asStateFlow()

    // 보낸 친구 요청
    private val _sentRequests = MutableStateFlow<List<FriendResponse>>(emptyList())
    val sentRequests: StateFlow<List<FriendResponse>> = _sentRequests.asStateFlow()

    init {
        loadFriendsList()
        loadFriendRequests()
    }

    // 친구 목록 조회
    fun loadFriendsList() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val friends = friendRepository.getFriendsList()
                _friendsList.value = friends
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "친구 목록을 불러오는데 실패했습니다."
                )
            }
        }
    }

    // 친구 요청 목록들 조회
    fun loadFriendRequests() {
        viewModelScope.launch {
            try {
                // 받은 요청
                val received = friendRepository.getReceivedFriendRequests()
                _receivedRequests.value = received

                // 보낸 요청
                val sent = friendRepository.getSentFriendRequests()
                _sentRequests.value = sent
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "친구 요청 목록을 불러오는데 실패했습니다."
                )
            }
        }
    }

    // 친구 요청 보내기
    fun sendFriendRequest(targetUserId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                friendRepository.sendFriendRequest(targetUserId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "친구 요청을 보냈습니다."
                )
                // 보낸 요청 목록 새로고침
                loadFriendRequests()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "친구 요청을 보내는데 실패했습니다."
                )
            }
        }
    }

    // 친구 요청 수락
    fun acceptFriendRequest(friendshipId: Long) {
        viewModelScope.launch {
            try {
                friendRepository.acceptFriendRequest(friendshipId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "친구 요청을 수락했습니다."
                )
                // 데이터 새로고침
                loadFriendsList()
                loadFriendRequests()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "친구 요청 수락에 실패했습니다."
                )
            }
        }
    }

    // 친구 요청 거절
    fun rejectFriendRequest(friendshipId: Long) {
        viewModelScope.launch {
            try {
                friendRepository.rejectFriendRequest(friendshipId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "친구 요청을 거절했습니다."
                )
                // 받은 요청 목록 새로고침
                loadFriendRequests()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "친구 요청 거절에 실패했습니다."
                )
            }
        }
    }

    // 친구 끊기
    fun removeFriend(friendshipId: Long) {
        viewModelScope.launch {
            try {
                friendRepository.removeFriend(friendshipId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "친구 관계를 해제했습니다."
                )
                // 친구 목록 새로고침
                loadFriendsList()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "친구 관계 해제에 실패했습니다."
                )
            }
        }
    }

    // 검색 필터링 (로컬에서 처리)
    fun searchFriends(query: String): List<FriendResponse> {
        return if (query.isEmpty()) {
            _friendsList.value
        } else {
            _friendsList.value.filter { friend ->
                friend.nickname.contains(query, ignoreCase = true) ||
                        friend.userId.contains(query, ignoreCase = true)
            }
        }
    }

    // 에러 메시지 클리어
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // 성공 메시지 클리어
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // 위치 공유 요청 보내기
    fun sendLocationShareRequest(targetUserId: String) {
        Log.d("FriendViewModel", "위치 공유 요청 시작: targetUserId=$targetUserId")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val result = locationRepository.sendLocationShareRequest(targetUserId)
                Log.d("FriendViewModel", "위치 공유 요청 성공: $result")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "위치 공유 요청을 보냈습니다."
                )
            } catch (e: Exception) {
                Log.e("FriendViewModel", "위치 공유 요청 실패", e)
                val errorMessage = when (e) {
                    is retrofit2.HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e("FriendViewModel", "HTTP ${e.code()} 에러: $errorBody")
                        "위치 공유 요청 실패 (HTTP ${e.code()}): $errorBody"
                    }
                    else -> e.message ?: "위치 공유 요청을 보내는데 실패했습니다."
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    // 새로고침
    fun refresh() {
        loadFriendsList()
        loadFriendRequests()
    }
}

// UI 상태 데이터 클래스
data class FriendUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)