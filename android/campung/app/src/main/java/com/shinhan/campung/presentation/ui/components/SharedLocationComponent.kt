package com.shinhan.campung.presentation.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.SharedLocation
import com.shinhan.campung.presentation.ui.map.SharedLocationMarkerManager
import android.util.Log

/**
 * 친구 위치공유 관련 기능을 모듈화한 컴포넌트
 */
@Composable
fun SharedLocationComponent(
    map: NaverMap?,
    sharedLocations: List<SharedLocation>,
    isVisible: Boolean = true,
    onMarkerClick: ((SharedLocation) -> Unit)? = null
) {
    val context = LocalContext.current
    val markerManager = remember(map) {
        map?.let { SharedLocationMarkerManager() }
    }
    
    // 위치 공유 마커 업데이트
    LaunchedEffect(map, sharedLocations, isVisible) {
        if (isVisible && map != null && markerManager != null) {
            Log.d("SharedLocationComponent", "위치 공유 마커 업데이트: ${sharedLocations.size}개")
            markerManager.updateSharedLocationMarkers(map, sharedLocations)
        } else if (!isVisible && markerManager != null) {
            Log.d("SharedLocationComponent", "위치 공유 마커 숨김")
            markerManager.clearAllMarkers()
        }
    }
    
    // 컴포넌트 정리 시 마커 제거
    DisposableEffect(map) {
        onDispose {
            markerManager?.clearAllMarkers()
        }
    }
}

/**
 * 위치공유 상태를 관리하는 클래스
 */
class SharedLocationState {
    private var _sharedLocations by mutableStateOf<List<SharedLocation>>(emptyList())
    private var _isEnabled by mutableStateOf(false)
    private var _selectedLocation by mutableStateOf<SharedLocation?>(null)
    
    val sharedLocations: List<SharedLocation> get() = _sharedLocations
    val isEnabled: Boolean get() = _isEnabled
    val selectedLocation: SharedLocation? get() = _selectedLocation
    
    fun updateLocations(locations: List<SharedLocation>) {
        _sharedLocations = locations
        Log.d("SharedLocationState", "위치 업데이트: ${locations.size}개")
    }
    
    fun enableSharing() {
        _isEnabled = true
        Log.d("SharedLocationState", "위치 공유 활성화")
    }
    
    fun disableSharing() {
        _isEnabled = false
        _sharedLocations = emptyList()
        _selectedLocation = null
        Log.d("SharedLocationState", "위치 공유 비활성화")
    }
    
    fun selectLocation(location: SharedLocation?) {
        _selectedLocation = location
        Log.d("SharedLocationState", "위치 선택: ${location?.userName}")
    }
    
    fun addLocation(location: SharedLocation) {
        _sharedLocations = _sharedLocations + location
        Log.d("SharedLocationState", "위치 추가: ${location.userName}")
    }
    
    fun removeLocation(shareId: String) {
        _sharedLocations = _sharedLocations.filter { it.shareId != shareId }
        if (_selectedLocation?.shareId == shareId) {
            _selectedLocation = null
        }
        Log.d("SharedLocationState", "위치 제거: $shareId")
    }
}

/**
 * SharedLocationState를 생성하는 컴포저블 함수
 */
@Composable
fun rememberSharedLocationState(): SharedLocationState {
    return remember { SharedLocationState() }
}

/**
 * 위치공유 마커 설정
 */
data class SharedLocationConfig(
    val showUserNames: Boolean = true,
    val markerSize: Int = 80, // dp
    val captionSize: Float = 12f,
    val captionColor: Int = 0xFF0000FF.toInt(),
    val enableClickEvents: Boolean = true
)

/**
 * 고급 위치공유 컴포넌트 - 더 많은 제어 옵션 제공
 */
@Composable
fun AdvancedSharedLocationComponent(
    map: NaverMap?,
    locationState: SharedLocationState,
    config: SharedLocationConfig = SharedLocationConfig(),
    onLocationClick: ((SharedLocation) -> Unit)? = null,
    onLocationLongClick: ((SharedLocation) -> Unit)? = null
) {
    SharedLocationComponent(
        map = map,
        sharedLocations = if (locationState.isEnabled) locationState.sharedLocations else emptyList(),
        isVisible = locationState.isEnabled,
        onMarkerClick = onLocationClick
    )
    
    // 선택된 위치 처리
    locationState.selectedLocation?.let { selected ->
        LaunchedEffect(selected) {
            Log.d("AdvancedSharedLocationComponent", "선택된 위치: ${selected.userName}")
            // 여기에 선택된 위치에 대한 추가 처리 로직을 구현할 수 있음
        }
    }
}