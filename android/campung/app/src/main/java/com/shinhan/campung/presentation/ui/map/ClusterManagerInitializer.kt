package com.shinhan.campung.presentation.ui.map

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.presentation.viewmodel.MapViewModel
import com.shinhan.campung.presentation.ui.components.TooltipType

class ClusterManagerInitializer(
    private val context: Context,
    private val mapViewModel: MapViewModel
) {
    
    // 새로운 통합 상호작용 컨트롤러
    private val interactionController = MapInteractionController(mapViewModel)
    
    fun createClusterManager(
        naverMap: NaverMap,
        mapContainer: ViewGroup? = null,
        onHighlightedContentChanged: (MapContent?) -> Unit
    ): Pair<MapClusterManager, MapInteractionController> {
        // 상호작용 컨트롤러에 NaverMap 설정
        interactionController.setNaverMap(naverMap)
        
        val manager = MapClusterManager(context, naverMap, mapContainer).also { manager ->
            manager.setupClustering()
            
            // 마커 클릭 → 상호작용 컨트롤러로 위임
            manager.onMarkerClick = { mapContent ->
                interactionController.onMarkerClick(mapContent)
            }
            
            // 클러스터 클릭 → 상호작용 컨트롤러로 위임
            manager.onClusterClick = { clusterContents ->
                interactionController.onClusterClick(clusterContents)
            }
            
            // 중앙 마커 변경은 기존 콜백만 유지 (상호작용 컨트롤러는 카메라 리스너에서 처리)
            manager.onCenterMarkerChanged = { centerContent ->
                onHighlightedContentChanged(centerContent)
            }
            
            // 툴팁 콜백 제거 (상호작용 컨트롤러에서 직접 처리)
            manager.onShowTooltip = null
            manager.onHideTooltip = null
            
            Log.d("ClusterManagerInitializer", "✅ ClusterManager 생성 완료 - 새로운 상호작용 시스템 적용")
        }
        
        return Pair(manager, interactionController)
    }
}