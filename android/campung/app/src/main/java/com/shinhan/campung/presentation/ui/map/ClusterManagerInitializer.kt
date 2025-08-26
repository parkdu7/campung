package com.shinhan.campung.presentation.ui.map

import android.content.Context
import android.util.Log
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.MapContent
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
                Log.d("ClusterManagerInitializer", "🎯 [FLOW] 마커 클릭 시작: ${mapContent.title} (ID: ${mapContent.contentId})")
                Log.d("ClusterManagerInitializer", "🔍 [DEBUG] mapViewModel 객체: $mapViewModel")
                
                try {
                    if (mapViewModel.isMarkerSelected(mapContent)) {
                        // 이미 선택된 마커 클릭 시 선택 해제
                        Log.d("ClusterManagerInitializer", "⚠️ [FLOW] 이미 선택된 마커 클릭 - 선택 해제 호출")
                        mapViewModel.clearSelectedMarker()
                    } else {
                        // 새 마커 선택
                        Log.d("ClusterManagerInitializer", "✅ [FLOW] 새 마커 선택 - selectMarker() 호출: ${mapContent.title}")
                        mapViewModel.selectMarker(mapContent)
                    }
                } catch (e: Exception) {
                    Log.e("ClusterManagerInitializer", "❌ [ERROR] 마커 클릭 처리 중 예외 발생", e)
                }
                Log.d("ClusterManagerInitializer", "🔚 [FLOW] 마커 클릭 처리 완료")
            }
            
            // 클러스터 클릭 이벤트 처리
            manager.onClusterClick = { clusterContents ->
                Log.d("ClusterManagerInitializer", "🎯 [FLOW] 클러스터 클릭 시작: ${clusterContents.size}개 아이템")
                Log.d("ClusterManagerInitializer", "📋 [FLOW] 클러스터 내용 ID들: ${clusterContents.map { it.contentId }}")
                Log.d("ClusterManagerInitializer", "🔍 [DEBUG] mapViewModel 객체: $mapViewModel")
                
                try {
                    Log.d("ClusterManagerInitializer", "✅ [FLOW] selectCluster() 호출")
                    // 클러스터 클릭 시 바텀시트에 클러스터 내용들 표시
                    mapViewModel.selectCluster(clusterContents)
                } catch (e: Exception) {
                    Log.e("ClusterManagerInitializer", "❌ [ERROR] 클러스터 클릭 처리 중 예외 발생", e)
                }
                Log.d("ClusterManagerInitializer", "🔚 [FLOW] 클러스터 클릭 처리 완료")
            }
            
            // 중앙 마커 변경 이벤트 처리
            manager.onCenterMarkerChanged = { centerContent ->
                onHighlightedContentChanged(centerContent)
            }
            
            Log.d("ClusterManagerInitializer", "ClusterManager 생성됨")
        }
    }
}