package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.naver.maps.map.NaverMap
import com.shinhan.campung.presentation.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Dispatchers

class MapCameraListener(
    private val mapViewModel: MapViewModel,
    private val clusterManager: MapClusterManager?,
    private val interactionController: MapInteractionController
) {
    
    private var lastZoomLevel = 0.0
    private var lastCameraChangeTime = 0L
    private var clusteringUpdateJob: kotlinx.coroutines.Job? = null
    private var isClusteringUpdate = false
    
    // 애니메이션 중 로딩 방지를 위한 상태 추가
    private var isAnimationInProgress = false
    
    fun createCameraChangeListener() = NaverMap.OnCameraChangeListener { reason, animated ->
        val currentTime = System.currentTimeMillis()
        
        // 쓰로틀링: 200ms 이내 연속 호출 방지
        if (currentTime - lastCameraChangeTime < 200) {
            return@OnCameraChangeListener
        }
        lastCameraChangeTime = currentTime
        Log.d("MapCameraListener", "📹 카메라 변경: reason=$reason, animated=$animated")
        
        val map = clusterManager?.naverMap ?: return@OnCameraChangeListener
        val center = map.cameraPosition.target
        val currentZoom = map.cameraPosition.zoom
        
        // 애니메이션 상태 감지 (마커 클릭, 클러스터 이동 등)
        val isClusterMoving = clusterManager?.isClusterMoving == true
        val isMarkerAnimation = animated && (reason == 2 || reason == 1) // GESTURE(1) 또는 DEVELOPER(2)
        isAnimationInProgress = isClusterMoving || isMarkerAnimation
        
        if (isAnimationInProgress) {
            Log.d("MapCameraListener", "🚫 애니메이션 중 - 데이터 로딩 스킵 (클러스터이동=$isClusterMoving, 마커애니메이션=$isMarkerAnimation)")
        }
        
        // 1. 줌 레벨 변경시 클러스터링 업데이트 - 더 큰 변화에만 반응하고 디바운스 적용
        if (kotlin.math.abs(currentZoom - lastZoomLevel) > 1.0) { // 0.5 → 1.0으로 변경
            lastZoomLevel = currentZoom
            Log.d("MapCameraListener", "줌 변경: $currentZoom → 클러스터링 업데이트 예약")
            
            // 이전 업데이트 Job 취소
            clusteringUpdateJob?.cancel()
            
            // 300ms 디바운스 적용 - 메인 스레드에서 실행
            clusteringUpdateJob = MainScope().launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(300)
                
                isClusteringUpdate = true
                Log.d("MapCameraListener", "클러스터링 업데이트 실행")
                
                // 선택된 마커 정보 백업
                val wasSelectedContent = clusterManager?.selectedContent
                
                clusterManager?.updateMarkers(mapViewModel.mapContents, mapViewModel.mapRecords) {
                    // 클러스터링 완료 후 선택 상태 복원
                    wasSelectedContent?.let { content ->
                        Log.d("MapCameraListener", "선택 상태 복원: ${content.title}")
                        clusterManager.selectMarker(content)
                    }
                    isClusteringUpdate = false
                }
            }
        }
        
        // 2. 사용자 드래그시 바텀시트 축소
        if (reason == -1) { // 사용자 드래그
            mapViewModel.onMapMove()
        }
        
        // 3. 상호작용 컨트롤러에 카메라 변경 알림 (애니메이션 중이 아닐 때만)
        if (clusterManager?.isClusterMoving == false && !isClusteringUpdate && !isAnimationInProgress) {
            interactionController.onCameraChanged(clusterManager)
        } else if (isAnimationInProgress) {
            Log.d("MapCameraListener", "🎬 애니메이션 진행 중 - 인터랙션 컨트롤러 호출 스킵")
        }
    }
}