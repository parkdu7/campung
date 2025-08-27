package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.naver.maps.map.NaverMap
import com.shinhan.campung.presentation.viewmodel.MapViewModel

class MapCameraListener(
    private val mapViewModel: MapViewModel,
    private val clusterManager: MapClusterManager?,
    private val interactionController: MapInteractionController
) {
    
    private var lastZoomLevel = 0.0
    private var lastCameraChangeTime = 0L
    
    fun createCameraChangeListener() = NaverMap.OnCameraChangeListener { reason, animated ->
        val currentTime = System.currentTimeMillis()
        
        // 쓰로틀링: 200ms 이내 연속 호출 방지
        if (currentTime - lastCameraChangeTime < 200) {
            return@OnCameraChangeListener
        }
        lastCameraChangeTime = currentTime
        Log.d("MapCameraListener", "📹 카메라 변경: reason=$reason")
        
        val map = clusterManager?.naverMap ?: return@OnCameraChangeListener
        val center = map.cameraPosition.target
        val currentZoom = map.cameraPosition.zoom
        
        // 1. 줌 레벨 변경시 클러스터링 업데이트
        if (kotlin.math.abs(currentZoom - lastZoomLevel) > 0.5) {
            lastZoomLevel = currentZoom
            Log.d("MapCameraListener", "줌 변경: $currentZoom → 클러스터링 업데이트")
            clusterManager?.updateMarkers(mapViewModel.mapContents)
        }
        
        // 2. 사용자 드래그시 바텀시트 축소
        if (reason == -1) { // 사용자 드래그
            mapViewModel.onMapMove()
        }
        
        // 4. 상호작용 컨트롤러에 카메라 변경 알림 (중앙 마커 추적 등)
        if (clusterManager?.isClusterMoving == false) {
            interactionController.onCameraChanged(clusterManager)
        }
    }
}