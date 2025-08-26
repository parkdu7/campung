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
    
    fun createCameraChangeListener() = NaverMap.OnCameraChangeListener { reason, animated ->
        val currentTime = System.currentTimeMillis()
        Log.d("MapCameraListener", "🔍 [DEBUG] 카메라 변경 - reason: $reason, animated: $animated")
        
        // 쓰로틀링: 200ms 이내 연속 호출 방지
        if (currentTime - lastCameraChangeTime < 200) {
            Log.d("MapCameraListener", "⏸️ [FLOW] 쓰로틀링으로 카메라 변경 이벤트 무시 - ${currentTime - lastCameraChangeTime}ms 전")
            return@OnCameraChangeListener
        }
        lastCameraChangeTime = currentTime
        Log.d("MapCameraListener", "📹 [FLOW] 카메라 변경 이벤트 처리 시작 - reason: $reason")
        
        val map = clusterManager?.naverMap ?: return@OnCameraChangeListener
        val center = map.cameraPosition.target
        val currentZoom = map.cameraPosition.zoom
        
        // 줌 레벨이 변경된 경우에만 클러스터링 업데이트
        if (kotlin.math.abs(currentZoom - lastZoomLevel) > 0.5) {
            lastZoomLevel = currentZoom
            Log.d("MapCameraListener", "줌 레벨 변경: $currentZoom, 마커 개수: ${mapViewModel.mapContents.size}")
            clusterManager?.updateMarkers(mapViewModel.mapContents)
        }
        
        // 지도 이동 시 바텀시트 닫기 (사용자 제스처일 때만)
        // reason 값: -1=사용자 드래그, 0=프로그래밍적 줌인
        when (reason) {
            -1 -> {
                Log.d("MapCameraListener", "🗺️ [FLOW] 사용자 드래그 감지 (reason=-1) - onMapMove() 호출")
                mapViewModel.onMapMove()
            }
            0 -> {
                Log.d("MapCameraListener", "🔧 [FLOW] 프로그래밍적 줌인 감지 (reason=0) - onMapMove() 호출 안함")
            }
            else -> {
                Log.d("MapCameraListener", "❓ [FLOW] 기타 이동 감지 (reason=$reason) - onMapMove() 호출")
                mapViewModel.onMapMove()
            }
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