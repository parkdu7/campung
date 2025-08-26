package com.shinhan.campung.presentation.ui.map

import android.content.Context
import android.util.Log
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.remote.response.MapContent
import com.shinhan.campung.presentation.viewmodel.MapViewModel

class ClusterManagerInitializer(
    private val context: Context,
    private val mapViewModel: MapViewModel
) {
    
    fun createClusterManager(
        naverMap: NaverMap,
        onHighlightedContentChanged: (MapContent?) -> Unit
    ): MapClusterManager {
        return MapClusterManager(context, naverMap).also { manager ->
            manager.setupClustering()
            
            // 마커 클릭 이벤트 처리 - ViewModel과 연동
            manager.onMarkerClick = { mapContent ->
                Log.d("ClusterManagerInitializer", "마커 클릭: ${mapContent.title}")
                if (mapViewModel.isMarkerSelected(mapContent)) {
                    // 이미 선택된 마커 클릭 시 선택 해제
                    Log.d("ClusterManagerInitializer", "이미 선택된 마커 클릭 - 선택 해제")
                    mapViewModel.clearSelectedMarker()
                } else {
                    // 새 마커 선택
                    Log.d("ClusterManagerInitializer", "새 마커 선택: ${mapContent.title}")
                    mapViewModel.selectMarker(mapContent)
                }
            }
            
            // 클러스터 클릭 이벤트 처리
            manager.onClusterClick = { clusterContents ->
                Log.d("ClusterManagerInitializer", "클러스터 클릭: ${clusterContents.size}개 아이템")
                // 클러스터 클릭 시 선택 해제
                mapViewModel.clearSelectedMarker()
            }
            
            // 중앙 마커 변경 이벤트 처리
            manager.onCenterMarkerChanged = { centerContent ->
                onHighlightedContentChanged(centerContent)
            }
            
            Log.d("ClusterManagerInitializer", "ClusterManager 생성됨")
        }
    }
}