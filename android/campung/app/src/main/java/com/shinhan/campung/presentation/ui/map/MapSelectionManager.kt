package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.presentation.viewmodel.MapViewModel

/**
 * 지도상의 마커/클러스터 선택 상태를 통합 관리하는 클래스
 * - 개별 마커 선택 vs 클러스터 선택 충돌 방지
 * - 툴팁 표시 로직 통합 관리
 */
class MapSelectionManager(
    private val mapViewModel: MapViewModel
) {
    
    // NaverMap 참조 (툴팁 표시를 위해)
    private var naverMapRef: com.naver.maps.map.NaverMap? = null
    
    /**
     * NaverMap 참조 설정
     */
    fun setNaverMap(naverMap: com.naver.maps.map.NaverMap) {
        this.naverMapRef = naverMap
    }
    
    private val tag = "MapSelectionManager"
    
    // 현재 선택 상태
    sealed class SelectionState {
        object None : SelectionState()
        data class IndividualMarker(val content: MapContent) : SelectionState()
        data class Cluster(val contents: List<MapContent>) : SelectionState()
    }
    
    private var currentSelection: SelectionState = SelectionState.None
    
    /**
     * 개별 마커 선택
     */
    fun selectIndividualMarker(content: MapContent) {
        Log.d(tag, "🎯 개별 마커 선택: ${content.title}")
        
        // 이전 선택 상태 정리
        clearPreviousSelection()
        
        // 새로운 선택 설정
        currentSelection = SelectionState.IndividualMarker(content)
        
        // ViewModel에 알림
        mapViewModel.selectMarker(content)
        
        // 선택된 마커 툴팁 표시
        naverMapRef?.let { naverMap ->
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                mapViewModel.showTooltip(content, naverMap, com.shinhan.campung.presentation.ui.components.TooltipType.CLICK)
            }, 150) // 마커 애니메이션 후 툴팁 표시
        }
        
        Log.d(tag, "✅ 개별 마커 선택 완료")
    }
    
    /**
     * 클러스터 선택
     */
    fun selectCluster(contents: List<MapContent>) {
        Log.d(tag, "🎯🎯🎯 클러스터 선택 시작: ${contents.size}개 항목")
        
        // 이전 선택 상태 정리 (개별 마커 툴팁 등 숨김)
        Log.d(tag, "이전 상태 정리 중...")
        clearPreviousSelection()
        
        // 새로운 선택 설정
        currentSelection = SelectionState.Cluster(contents)
        Log.d(tag, "현재 선택 상태 변경됨: ${currentSelection}")
        
        // 즉시 툴팁 숨김 (강제)
        Log.d(tag, "강제 툴팁 숨김 호출")
        hideFocusTooltip()
        
        // ViewModel에 알림
        mapViewModel.selectCluster(contents)
        
        Log.d(tag, "✅✅✅ 클러스터 선택 완료")
    }
    
    /**
     * 모든 선택 해제
     */
    fun clearSelection() {
        Log.d(tag, "🔄 모든 선택 해제")
        
        clearPreviousSelection()
        currentSelection = SelectionState.None
        
        // ViewModel 상태 정리
        mapViewModel.clearSelectedMarker()
        
        Log.d(tag, "✅ 선택 해제 완료")
    }
    
    /**
     * 중앙 마커 변경 처리 (카메라 이동시)
     * - 클러스터가 선택된 상태에서는 개별 마커 툴팁을 표시하지 않음
     */
    fun handleCenterMarkerChange(centerContent: MapContent?) {
        // 스마트 캐스트 문제 해결을 위해 로컬 변수로 복사
        val selection = currentSelection
        
        when (selection) {
            is SelectionState.None -> {
                // 아무것도 선택되지 않은 상태 → 중앙 마커 툴팁 표시 허용
                if (centerContent != null) {
                    Log.d(tag, "🎯 중앙 마커 툴팁 표시: ${centerContent.title}")
                    // 포커스 툴팁 표시 (선택 툴팁과 다름)
                    showFocusTooltip(centerContent)
                } else {
                    Log.d(tag, "🫥 중앙 마커 없음 - 툴팁 숨김")
                    hideFocusTooltip()
                }
            }
            is SelectionState.IndividualMarker -> {
                // 개별 마커가 선택된 상태 → 선택된 마커가 아닌 경우에만 포커스 툴팁
                if (centerContent != null && centerContent.contentId != selection.content.contentId) {
                    Log.d(tag, "🎯 다른 마커에 포커스 툴팁 표시: ${centerContent.title}")
                    showFocusTooltip(centerContent)
                } else {
                    hideFocusTooltip()
                }
            }
            is SelectionState.Cluster -> {
                // 클러스터가 선택된 상태 → 개별 마커 툴팁 표시 금지
                Log.d(tag, "🚫🚫🚫 클러스터 선택 중 - 개별 마커 툴팁 표시 안함 (centerContent: ${centerContent?.title})")
                hideFocusTooltip()
            }
        }
    }
    
    /**
     * 현재 선택 상태 확인
     */
    fun isClusterSelected(): Boolean {
        return currentSelection is SelectionState.Cluster
    }
    
    fun isIndividualMarkerSelected(): Boolean {
        return currentSelection is SelectionState.IndividualMarker
    }
    
    fun getCurrentSelection(): SelectionState {
        return currentSelection
    }
    
    /**
     * 이전 선택 상태 정리 (내부 함수)
     */
    private fun clearPreviousSelection() {
        // 스마트 캐스트 문제 해결을 위해 로컬 변수로 복사
        val selection = currentSelection
        
        when (selection) {
            is SelectionState.IndividualMarker -> {
                Log.d(tag, "이전 개별 마커 선택 정리")
                hideFocusTooltip()
            }
            is SelectionState.Cluster -> {
                Log.d(tag, "이전 클러스터 선택 정리")
                hideFocusTooltip()
            }
            is SelectionState.None -> {
                Log.d(tag, "정리할 이전 선택 없음")
            }
        }
    }
    
    /**
     * 포커스 툴팁 표시 (중앙 마커용)
     */
    private fun showFocusTooltip(content: MapContent) {
        naverMapRef?.let { naverMap ->
            mapViewModel.showTooltip(content, naverMap, com.shinhan.campung.presentation.ui.components.TooltipType.FOCUS)
            Log.d(tag, "포커스 툴팁 표시: ${content.title}")
        } ?: Log.w(tag, "NaverMap 참조가 없어서 툴팁 표시 불가")
    }
    
    /**
     * 포커스 툴팁 숨김
     */
    private fun hideFocusTooltip() {
        mapViewModel.hideTooltip()
        Log.d(tag, "포커스 툴팁 숨김")
    }
}