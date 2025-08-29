package com.shinhan.campung.presentation.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap

/**
 * 위치 마커 관리를 위한 모듈화된 컴포넌트
 * CampusMapCard와 FullMapScreen에서 재사용 가능
 */
@Composable
fun LocationMarkerManager(
    map: NaverMap?,
    userLocation: LatLng?,
    hasLocationPermission: Boolean,
    enableLottieAnimation: Boolean = true
) {
    val context = LocalContext.current
    
    // 지도와 위치가 준비되었을 때만 마커 표시
    map?.let { naverMap ->
        userLocation?.let { location ->
            if (hasLocationPermission) {
                if (enableLottieAnimation) {
                    // Lottie 애니메이션 마커 사용
                    MyLocationMarker(
                        map = naverMap,
                        location = location
                    )
                } else {
                    // 기본 location overlay 사용
                    LaunchedEffect(location) {
                        naverMap.locationOverlay.apply {
                            isVisible = true
                            position = location
                        }
                    }
                }
            }
        }
    }
    
    // 권한이 없거나 위치가 없을 때는 overlay 숨기기
    LaunchedEffect(hasLocationPermission, userLocation) {
        if (!hasLocationPermission || userLocation == null) {
            map?.locationOverlay?.isVisible = false
        }
    }
}

/**
 * 위치 마커의 설정을 관리하는 데이터 클래스
 */
data class LocationMarkerConfig(
    val enableLottieAnimation: Boolean = true,
    val markerSize: Int = 70, // dp
    val zIndex: Float = 0f
)

/**
 * 위치 마커 상태를 관리하는 클래스
 */
class LocationMarkerState {
    private var _isVisible by mutableStateOf(false)
    private var _location by mutableStateOf<LatLng?>(null)
    private var _hasPermission by mutableStateOf(false)
    
    val isVisible: Boolean get() = _isVisible
    val location: LatLng? get() = _location  
    val hasPermission: Boolean get() = _hasPermission
    
    fun updateLocation(newLocation: LatLng?) {
        _location = newLocation
        updateVisibility()
    }
    
    fun updatePermission(hasPermission: Boolean) {
        _hasPermission = hasPermission
        updateVisibility()
    }
    
    private fun updateVisibility() {
        _isVisible = _hasPermission && _location != null
    }
    
    fun hide() {
        _isVisible = false
    }
    
    fun show() {
        if (_hasPermission && _location != null) {
            _isVisible = true
        }
    }
}

/**
 * LocationMarkerState를 생성하는 컴포저블 함수
 */
@Composable
fun rememberLocationMarkerState(): LocationMarkerState {
    return remember { LocationMarkerState() }
}