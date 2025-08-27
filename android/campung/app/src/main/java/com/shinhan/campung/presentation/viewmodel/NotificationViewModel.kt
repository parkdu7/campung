package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.repository.FriendRepository
import com.shinhan.campung.data.repository.LocationRepository
import com.shinhan.campung.data.repository.NotificationRepository
import com.shinhan.campung.data.service.LocationService
import com.shinhan.campung.presentation.ui.screens.NotificationItem
import com.shinhan.campung.presentation.ui.screens.NotificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val friendRepository: FriendRepository,
    private val locationRepository: LocationRepository,
    private val locationService: LocationService
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // 알림 목록
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    // 알림 목록 조회
    fun loadNotifications() {
        viewModelScope.launch {
            try {
                Log.d("NotificationVM", "알림 목록 조회 시작")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val notifications = notificationRepository.getNotifications()
                Log.d("NotificationVM", "서버 응답: ${notifications.size}개 알림")
                _notifications.value = notifications.map { notification ->
                    Log.d("NotificationVM", "알림: ${notification.type} - ${notification.title}")
                    NotificationItem(
                        id = notification.notificationId,
                        type = notification.type,
                        title = notification.title,
                        message = notification.message,
                        data = notification.data,
                        isRead = notification.isRead,
                        createdAt = notification.createdAt,
                        requesterId = extractRequesterIdFromData(notification.data, notification.type)
                    )
                }
                Log.d("NotificationVM", "최종 알림 리스트: ${_notifications.value.size}개")
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e("NotificationVM", "알림 조회 실패", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "알림을 불러오는데 실패했습니다."
                )
            }
        }
    }

    // 친구 요청 수락
    fun acceptFriendRequest(notificationId: Long) {
        viewModelScope.launch {
            try {
                val notification = _notifications.value.find { it.id == notificationId }
                val friendshipId = extractFriendshipIdFromData(notification?.data)

                if (friendshipId != null) {
                    friendRepository.acceptFriendRequest(friendshipId)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "친구 요청을 수락했습니다."
                    )
                    // 알림을 읽음 처리하고 목록에서 제거
                    markAsReadAndRemove(notificationId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "친구 요청 정보를 찾을 수 없습니다."
                    )
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("이미 처리된 요청") == true -> "이미 처리된 친구 요청입니다."
                    e.message?.contains("500") == true -> "이미 친구 관계이거나 처리된 요청입니다."
                    else -> e.message ?: "친구 요청 수락에 실패했습니다."
                }
                _uiState.value = _uiState.value.copy(error = errorMessage)
                // 에러가 발생해도 UI에서 해당 알림을 제거
                markAsReadAndRemove(notificationId)
            }
        }
    }

    // 친구 요청 거절
    fun rejectFriendRequest(notificationId: Long) {
        viewModelScope.launch {
            try {
                val notification = _notifications.value.find { it.id == notificationId }
                val friendshipId = extractFriendshipIdFromData(notification?.data)

                if (friendshipId != null) {
                    friendRepository.rejectFriendRequest(friendshipId)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "친구 요청을 거절했습니다."
                    )
                    // 알림을 읽음 처리하고 목록에서 제거
                    markAsReadAndRemove(notificationId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "친구 요청 정보를 찾을 수 없습니다."
                    )
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("이미 처리된 요청") == true -> "이미 처리된 친구 요청입니다."
                    e.message?.contains("500") == true -> "이미 친구 관계이거나 처리된 요청입니다."
                    else -> e.message ?: "친구 요청 거절에 실패했습니다."
                }
                _uiState.value = _uiState.value.copy(error = errorMessage)
                // 에러가 발생해도 UI에서 해당 알림을 제거
                markAsReadAndRemove(notificationId)
            }
        }
    }

    // 위치 공유 요청 수락
    fun acceptLocationShareRequest(notificationId: Long) {
        viewModelScope.launch {
            try {
                // 위치 권한 확인
                if (!locationService.hasLocationPermission()) {
                    _uiState.value = _uiState.value.copy(
                        error = "위치 권한이 필요합니다. 설정에서 권한을 허용해주세요."
                    )
                    return@launch
                }

                val notification = _notifications.value.find { it.id == notificationId }
                val locationRequestId = extractLocationRequestIdFromData(notification?.data)

                if (locationRequestId != null) {
                    Log.d("NotificationVM", "위치 공유 요청 수락 시작: locationRequestId=$locationRequestId")
                    locationRepository.acceptLocationShareRequest(locationRequestId)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "위치 공유 요청을 수락했습니다."
                    )
                    // 알림을 읽음 처리하고 목록에서 제거
                    markAsReadAndRemove(notificationId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "위치 공유 요청 정보를 찾을 수 없습니다."
                    )
                }
            } catch (e: Exception) {
                Log.e("NotificationVM", "위치 공유 요청 수락 실패", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "위치 공유 요청 수락에 실패했습니다."
                )
            }
        }
    }

    // 위치 공유 요청 거절
    fun rejectLocationShareRequest(notificationId: Long) {
        viewModelScope.launch {
            try {
                val notification = _notifications.value.find { it.id == notificationId }
                val locationRequestId = extractLocationRequestIdFromData(notification?.data)

                if (locationRequestId != null) {
                    locationRepository.rejectLocationShareRequest(locationRequestId)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "위치 공유 요청을 거절했습니다."
                    )
                    // 알림을 읽음 처리하고 목록에서 제거
                    markAsReadAndRemove(notificationId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "위치 공유 요청 정보를 찾을 수 없습니다."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "위치 공유 요청 거절에 실패했습니다."
                )
            }
        }
    }

    // 알림을 읽음 처리하고 목록에서 제거
    private fun markAsReadAndRemove(notificationId: Long) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
                // 로컬 상태에서도 제거
                _notifications.value = _notifications.value.filter { it.id != notificationId }
            } catch (e: Exception) {
                // 읽음 처리 실패해도 에러는 표시하지 않음
            }
        }
    }

    // JSON 데이터에서 요청자 ID 추출
    private fun extractRequesterIdFromData(data: String?, type: String): String? {
        if (data.isNullOrEmpty()) return null
        return try {
            // JSON 파싱 로직 (실제로는 Gson이나 kotlinx.serialization 사용)
            // 예: {"requesterId": "user123", "friendshipId": 456}
            when (type) {
                "friendRequest" -> {
                    // data에서 requesterId 추출
                    val regex = "\"requesterId\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    regex.find(data)?.groupValues?.get(1)
                }
                "location_share_request" -> {
                    // data에서 fromUserId 추출
                    val regex = "\"fromUserId\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    regex.find(data)?.groupValues?.get(1)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // JSON 데이터에서 친구관계 ID 추출
    private fun extractFriendshipIdFromData(data: String?): Long? {
        if (data.isNullOrEmpty()) return null
        return try {
            val regex = "\"friendshipId\"\\s*:\\s*(\\d+)".toRegex()
            regex.find(data)?.groupValues?.get(1)?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // JSON 데이터에서 위치 요청 ID 추출
    private fun extractLocationRequestIdFromData(data: String?): Long? {
        if (data.isNullOrEmpty()) return null
        return try {
            val regex = "\"shareRequestId\"\\s*:\\s*(\\d+)".toRegex()
            regex.find(data)?.groupValues?.get(1)?.toLongOrNull()
        } catch (e: Exception) {
            null
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

    // 새로고침
    fun refresh() {
        loadNotifications()
    }
}