package com.shinhan.campung.presentation.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.SharedLocation
import android.util.Log

/**
 * 친구 위치공유 통합 관리 컴포넌트
 * 데이터 관리, 상태 관리, UI 표시를 통합적으로 처리
 */
@Composable
fun FriendLocationSharingManager(
    map: NaverMap?,
    config: SharedLocationConfig = SharedLocationConfig(),
    onFriendLocationClick: ((SharedLocation) -> Unit)? = null
) {
    val context = LocalContext.current
    val dataManager = rememberSharedLocationDataManager()
    
    // 상태 수집
    val sharedLocations by dataManager.collectSharedLocationsAsState()
    val isSharingEnabled by dataManager.collectLocationSharingEnabledAsState()
    val selectedFriend by dataManager.collectSelectedFriendAsState()
    
    // 위치공유 컴포넌트 표시
    SharedLocationComponent(
        map = map,
        sharedLocations = sharedLocations,
        isVisible = isSharingEnabled,
        onMarkerClick = { location ->
            dataManager.selectFriend(location)
            onFriendLocationClick?.invoke(location)
            Log.d("FriendLocationSharingManager", "친구 위치 클릭: ${location.userName}")
        }
    )
    
    // 디버그 정보 로깅
    LaunchedEffect(sharedLocations, isSharingEnabled) {
        Log.d("FriendLocationSharingManager", 
            "상태 업데이트 - 공유활성화: $isSharingEnabled, 위치개수: ${sharedLocations.size}")
    }
}

/**
 * FullMapScreen에서 사용할 친구 위치공유 컴포넌트
 */
@Composable
fun FullMapFriendLocationManager(
    map: NaverMap?,
    onFriendClick: ((SharedLocation) -> Unit)? = null
) {
    FriendLocationSharingManager(
        map = map,
        config = SharedLocationConfig(
            showUserNames = true,
            markerSize = 100,
            enableClickEvents = true
        ),
        onFriendLocationClick = onFriendClick
    )
}

/**
 * CampusMapCard에서 사용할 친구 위치공유 컴포넌트 (더 작은 사이즈)
 */
@Composable
fun CampusMapFriendLocationManager(
    map: NaverMap?,
    onFriendClick: ((SharedLocation) -> Unit)? = null
) {
    FriendLocationSharingManager(
        map = map,
        config = SharedLocationConfig(
            showUserNames = false, // 작은 맵에서는 이름 숨김
            markerSize = 80, // CampusMap에서도 크기 증가
            captionSize = 10f,
            enableClickEvents = true
        ),
        onFriendLocationClick = onFriendClick
    )
}


    
    /**
     * 위치 간 거리 계산 (미터)
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371000.0 // 지구 반지름 (미터)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
    
    /**
     * 친구가 근처에 있는지 확인
     */
    fun isFriendNearby(
        myLocation: com.naver.maps.geometry.LatLng,
        friendLocation: SharedLocation,
        radiusMeters: Double = 100.0
    ): Boolean {
        val distance = calculateDistance(
            myLocation.latitude, myLocation.longitude,
            friendLocation.latitude, friendLocation.longitude
        )
        return distance <= radiusMeters
    }