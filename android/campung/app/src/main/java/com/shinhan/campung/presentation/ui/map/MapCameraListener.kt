package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.naver.maps.map.NaverMap
import com.shinhan.campung.presentation.viewmodel.MapViewModel

class MapCameraListener(
    private val mapViewModel: MapViewModel,
    private val clusterManager: MapClusterManager?
) {
    
    private var lastZoomLevel = 0.0
    private var lastCameraChangeTime = 0L
    
    fun createCameraChangeListener() = NaverMap.OnCameraChangeListener { _, _ ->
        val currentTime = System.currentTimeMillis()
        
        // 쓰로틀링: 200ms 이내 연속 호출 방지
        if (currentTime - lastCameraChangeTime < 200) {
            return@OnCameraChangeListener
        }
        lastCameraChangeTime = currentTime
        
        val map = clusterManager?.naverMap ?: return@OnCameraChangeListener
        val center = map.cameraPosition.target
        val currentZoom = map.cameraPosition.zoom
        
        // 줌 레벨이 변경된 경우에만 클러스터링 업데이트
        if (kotlin.math.abs(currentZoom - lastZoomLevel) > 0.5) {
            lastZoomLevel = currentZoom
            Log.d("MapCameraListener", "줌 레벨 변경: $currentZoom, 마커 개수: ${mapViewModel.mapContents.size}")
            clusterManager?.updateMarkers(mapViewModel.mapContents)
        }
        
        // API 요청 (디바운스는 ViewModel에서 처리)
        mapViewModel.loadMapContents(
            latitude = center.latitude,
            longitude = center.longitude
        )
        
        // 중앙 마커 찾기
        Log.d("MapCameraListener", "카메라 변경 - 중앙 마커 찾기 호출")
        clusterManager?.findCenterMarker()
    }
}