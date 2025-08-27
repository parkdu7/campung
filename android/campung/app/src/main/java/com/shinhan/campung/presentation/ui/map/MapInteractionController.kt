package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.presentation.viewmodel.MapViewModel
import com.shinhan.campung.presentation.ui.components.TooltipType

/**
 * 지도 상호작용을 통합 관리하는 단일 컨트롤러
 * - 모든 마커/클러스터 선택 로직 통합
 * - 툴팁 표시 상태 통합 관리
 * - 중앙 마커 추적 통합 관리
 */
class MapInteractionController(
    private val mapViewModel: MapViewModel
) {
    
    private val tag = "MapInteractionController"
    
    // NaverMap 참조
    private var naverMap: NaverMap? = null
    
    // 현재 상태 - 단순하게 3가지만
    sealed class InteractionState {
        object None : InteractionState()                           // 아무것도 선택 안됨
        data class MarkerSelected(val content: MapContent) : InteractionState()  // 개별 마커 선택됨
        data class ClusterSelected(val contents: List<MapContent>) : InteractionState() // 클러스터 선택됨
    }
    
    private var currentState: InteractionState = InteractionState.None
    
    // 중앙 추적 중인 마커 (선택과는 별개)
    private var centerMarker: MapContent? = null
    
    /**
     * NaverMap 설정
     */
    fun setNaverMap(naverMap: NaverMap) {
        this.naverMap = naverMap
        Log.d(tag, "NaverMap 설정됨")
    }
    
    /**
     * 개별 마커 클릭 처리
     */
    fun onMarkerClick(content: MapContent) {
        Log.d(tag, "🎯 마커 클릭: ${content.title}")
        
        when (val state = currentState) {
            is InteractionState.MarkerSelected -> {
                if (state.content.contentId == content.contentId) {
                    // 같은 마커 다시 클릭 → 선택 해제
                    Log.d(tag, "같은 마커 재클릭 → 선택 해제")
                    clearSelection()
                } else {
                    // 다른 마커 클릭 → 새로운 마커 선택
                    Log.d(tag, "다른 마커 선택")
                    selectMarker(content)
                }
            }
            else -> {
                // 새로운 마커 선택
                Log.d(tag, "새 마커 선택")
                selectMarker(content)
            }
        }
    }
    
    /**
     * 클러스터 클릭 처리  
     */
    fun onClusterClick(contents: List<MapContent>) {
        Log.d(tag, "🎯 클러스터 클릭: ${contents.size}개")
        
        // 항상 새로운 클러스터 선택
        selectCluster(contents)
    }
    
    /**
     * 카메라 변경 시 중앙 마커 업데이트
     */
    fun onCameraChanged(clusterManager: MapClusterManager?) {
        when (currentState) {
            is InteractionState.None -> {
                // 아무것도 선택 안된 상태에서만 중앙 마커 추적
                Log.d(tag, "중앙 마커 추적 활성화")
                updateCenterMarker(clusterManager)
            }
            is InteractionState.MarkerSelected -> {
                // 마커 선택된 상태에서도 중앙 마커 추적 (다른 마커 하이라이트용)
                Log.d(tag, "마커 선택 상태에서 중앙 마커 추적")
                updateCenterMarker(clusterManager)
            }
            is InteractionState.ClusterSelected -> {
                // 클러스터 선택된 상태에서는 중앙 마커 추적 중단
                Log.d(tag, "🚫 클러스터 선택 중 - 중앙 마커 추적 중단")
                clearCenterMarker()
            }
        }
    }
    
    /**
     * 지도 클릭 (빈 공간) 처리
     */
    fun onMapClick() {
        Log.d(tag, "🗺️ 지도 빈 공간 클릭 - 현재 상태: $currentState")
        
        // 현재 선택된 상태가 있으면 항상 해제
        when (currentState) {
            is InteractionState.ClusterSelected -> {
                Log.d(tag, "✅ 클러스터 선택 상태 → 해제 실행")
                clearSelection()
                return
            }
            is InteractionState.MarkerSelected -> {
                Log.d(tag, "✅ 마커 선택 상태 → 해제 실행")
                clearSelection()
                return
            }
            is InteractionState.None -> {
                Log.d(tag, "이미 빈 상태 → 변화 없음")
            }
        }
    }
    
    // === 내부 함수들 ===
    
    private fun selectMarker(content: MapContent) {
        Log.d(tag, "📌 마커 선택 실행: ${content.title}")
        
        // 이전 상태 정리
        clearPreviousState()
        
        // 새 상태 설정
        currentState = InteractionState.MarkerSelected(content)
        
        // ViewModel 알림
        mapViewModel.selectMarker(content)
        
        // 선택 툴팁 표시
        showClickTooltip(content)
        
        Log.d(tag, "✅ 마커 선택 완료")
    }
    
    private fun selectCluster(contents: List<MapContent>) {
        Log.d(tag, "📦 클러스터 선택 실행: ${contents.size}개")
        
        // 이전 상태 정리 
        clearPreviousState()
        
        // 새 상태 설정
        currentState = InteractionState.ClusterSelected(contents)
        
        // ViewModel 알림
        mapViewModel.selectCluster(contents)
        
        // 클러스터 선택시에는 툴팁 없음
        hideAllTooltips()
        
        Log.d(tag, "✅ 클러스터 선택 완료")
    }
    
    private fun clearSelection() {
        Log.d(tag, "🔄 선택 해제 시작 - 현재 상태: $currentState")
        
        // 현재 상태에 따라 구체적인 해제 작업
        when (currentState) {
            is InteractionState.ClusterSelected -> {
                Log.d(tag, "클러스터 선택 해제 중...")
            }
            is InteractionState.MarkerSelected -> {
                Log.d(tag, "마커 선택 해제 중...")
            }
            else -> {
                Log.d(tag, "이미 빈 상태")
            }
        }
        
        // 이전 상태 정리
        clearPreviousState()
        
        // 상태 초기화
        currentState = InteractionState.None
        
        // ViewModel 알림
        mapViewModel.clearSelectedMarker()
        
        Log.d(tag, "✅ 선택 해제 완료 - 새 상태: $currentState")
    }
    
    private fun updateCenterMarker(clusterManager: MapClusterManager?) {
        val newCenterMarker = clusterManager?.findCenterMarker()
        
        if (newCenterMarker?.contentId != centerMarker?.contentId) {
            Log.d(tag, "중앙 마커 변경: ${centerMarker?.title} → ${newCenterMarker?.title}")
            
            // 이전 중앙 마커 정리
            centerMarker?.let { hideFocusTooltip() }
            
            // 새 중앙 마커 설정
            centerMarker = newCenterMarker
            
            // 포커스 툴팁 표시 (딜레이 후 표시하여 애니메이션 개선)
            newCenterMarker?.let { content ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showFocusTooltipIfAllowed(content)
                }, 100) // 클릭 툴팁보다 짧은 딜레이로 빠르게 표시
            }
        }
    }
    
    private fun clearCenterMarker() {
        if (centerMarker != null) {
            Log.d(tag, "중앙 마커 추적 중단: ${centerMarker?.title}")
            hideFocusTooltip()
            centerMarker = null
        }
    }
    
    private fun showFocusTooltipIfAllowed(content: MapContent) {
        when (val state = currentState) {
            is InteractionState.None -> {
                // 아무것도 선택 안됨 → 포커스 툴팁 표시
                showFocusTooltip(content)
            }
            is InteractionState.MarkerSelected -> {
                // 마커 선택됨 → 선택된 마커와 다를 때만 포커스 툴팁
                if (state.content.contentId != content.contentId) {
                    showFocusTooltip(content)
                }
            }
            is InteractionState.ClusterSelected -> {
                // 클러스터 선택됨 → 포커스 툴팁 표시 안함
                Log.d(tag, "클러스터 선택 중이라 포커스 툴팁 표시 안함")
            }
        }
    }
    
    private fun clearPreviousState() {
        // 모든 툴팁 숨김
        hideAllTooltips()
        
        // 중앙 마커 정리
        centerMarker = null
    }
    
    // === 툴팁 관리 함수들 ===
    
    private fun showClickTooltip(content: MapContent) {
        naverMap?.let { map ->
            Log.d(tag, "🎯 선택 툴팁 표시: ${content.title}")
            mapViewModel.showTooltip(content, map, TooltipType.CLICK)
        }
    }
    
    private fun showFocusTooltip(content: MapContent) {
        naverMap?.let { map ->
            Log.d(tag, "👁️ 포커스 툴팁 표시: ${content.title}")
            mapViewModel.showTooltip(content, map, TooltipType.FOCUS)
        }
    }
    
    private fun hideFocusTooltip() {
        Log.d(tag, "👁️ 포커스 툴팁 숨김")
        mapViewModel.hideTooltip()
    }
    
    private fun hideAllTooltips() {
        Log.d(tag, "🫥 모든 툴팁 숨김")
        mapViewModel.hideTooltip()
    }
    
    // === 상태 조회 함수들 ===
    
    fun isClusterSelected(): Boolean = currentState is InteractionState.ClusterSelected
    fun isMarkerSelected(): Boolean = currentState is InteractionState.MarkerSelected
    fun getCurrentState(): InteractionState = currentState
}