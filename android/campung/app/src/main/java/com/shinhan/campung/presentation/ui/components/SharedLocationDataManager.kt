package com.shinhan.campung.presentation.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.shinhan.campung.data.model.SharedLocation
import android.util.Log

/**
 * 위치공유 데이터를 관리하는 매니저 클래스
 * 전역적으로 위치공유 상태를 관리하고 여러 화면에서 공유
 */
class SharedLocationDataManager {
    
    private val _sharedLocations = MutableStateFlow<List<SharedLocation>>(emptyList())
    val sharedLocations: StateFlow<List<SharedLocation>> = _sharedLocations.asStateFlow()
    
    private val _isLocationSharingEnabled = MutableStateFlow(false)
    val isLocationSharingEnabled: StateFlow<Boolean> = _isLocationSharingEnabled.asStateFlow()
    
    private val _selectedFriend = MutableStateFlow<SharedLocation?>(null)
    val selectedFriend: StateFlow<SharedLocation?> = _selectedFriend.asStateFlow()
    
    companion object {
        private const val TAG = "SharedLocationDataManager"
        
        @Volatile
        private var INSTANCE: SharedLocationDataManager? = null
        
        fun getInstance(): SharedLocationDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedLocationDataManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 위치공유 활성화/비활성화
     */
    fun setLocationSharingEnabled(enabled: Boolean) {
        _isLocationSharingEnabled.value = enabled
        Log.d(TAG, "위치공유 ${if (enabled) "활성화" else "비활성화"}")
        
        if (!enabled) {
            // 비활성화시 모든 데이터 초기화
            clearAllLocations()
        }
    }
    
    /**
     * 위치 목록 업데이트 (서버에서 받아온 데이터)
     */
    fun updateSharedLocations(locations: List<SharedLocation>) {
        _sharedLocations.value = locations
        Log.d(TAG, "위치 목록 업데이트: ${locations.size}개")
        
        // 선택된 친구가 목록에서 사라진 경우 선택 해제
        val selectedId = _selectedFriend.value?.shareId
        if (selectedId != null && locations.none { it.shareId == selectedId }) {
            _selectedFriend.value = null
            Log.d(TAG, "선택된 친구가 목록에서 제거됨: $selectedId")
        }
    }
    
    /**
     * 특정 친구 선택
     */
    fun selectFriend(friend: SharedLocation?) {
        _selectedFriend.value = friend
        Log.d(TAG, "친구 선택: ${friend?.userName ?: "없음"}")
    }
    
    /**
     * 새로운 위치 추가
     */
    fun addSharedLocation(location: SharedLocation) {
        val currentList = _sharedLocations.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.shareId == location.shareId }
        
        if (existingIndex >= 0) {
            // 기존 위치 업데이트
            currentList[existingIndex] = location
            Log.d(TAG, "위치 업데이트: ${location.userName}")
        } else {
            // 새로운 위치 추가
            currentList.add(location)
            Log.d(TAG, "새 위치 추가: ${location.userName}")
        }
        
        _sharedLocations.value = currentList
    }
    
    /**
     * 특정 위치 제거
     */
    fun removeSharedLocation(shareId: String) {
        val currentList = _sharedLocations.value.toMutableList()
        val removed = currentList.removeAll { it.shareId == shareId }
        
        if (removed) {
            _sharedLocations.value = currentList
            Log.d(TAG, "위치 제거: $shareId")
            
            // 제거된 위치가 선택된 친구인 경우 선택 해제
            if (_selectedFriend.value?.shareId == shareId) {
                _selectedFriend.value = null
            }
        }
    }
    
    /**
     * 모든 위치 데이터 초기화
     */
    fun clearAllLocations() {
        _sharedLocations.value = emptyList()
        _selectedFriend.value = null
        Log.d(TAG, "모든 위치 데이터 초기화")
    }
    
    /**
     * 특정 친구의 위치 가져오기
     */
    fun getLocationByShareId(shareId: String): SharedLocation? {
        return _sharedLocations.value.find { it.shareId == shareId }
    }
    
    /**
     * 현재 활성화된 위치공유 개수
     */
    fun getActiveLocationCount(): Int {
        return if (_isLocationSharingEnabled.value) _sharedLocations.value.size else 0
    }
}

/**
 * Compose에서 SharedLocationDataManager를 사용하기 위한 헬퍼 함수
 */
@Composable
fun rememberSharedLocationDataManager(): SharedLocationDataManager {
    return remember { SharedLocationDataManager.getInstance() }
}

/**
 * StateFlow를 State로 변환하는 헬퍼 함수들
 */
@Composable
fun SharedLocationDataManager.collectSharedLocationsAsState(): State<List<SharedLocation>> {
    return sharedLocations.collectAsState()
}

@Composable
fun SharedLocationDataManager.collectLocationSharingEnabledAsState(): State<Boolean> {
    return isLocationSharingEnabled.collectAsState()
}

@Composable
fun SharedLocationDataManager.collectSelectedFriendAsState(): State<SharedLocation?> {
    return selectedFriend.collectAsState()
}