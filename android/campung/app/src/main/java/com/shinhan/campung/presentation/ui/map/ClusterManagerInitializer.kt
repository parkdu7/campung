package com.shinhan.campung.presentation.ui.map

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.model.MapRecord
import com.shinhan.campung.presentation.viewmodel.MapViewModel
import com.shinhan.campung.presentation.ui.components.TooltipType

class ClusterManagerInitializer(
    private val context: Context,
    private val mapViewModel: MapViewModel
) {
    
    fun createClusterManager(
        naverMap: NaverMap,
        mapContainer: ViewGroup? = null,
        onHighlightedContentChanged: (MapContent?) -> Unit
    ): MapClusterManager {
        return MapClusterManager(context, naverMap, mapContainer).also { manager ->
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
            
            // 툴팁 콜백 연결
            manager.onShowTooltip = { content, type ->
                mapViewModel.showTooltip(content, naverMap, type)
            }
            
            manager.onHideTooltip = {
                mapViewModel.hideTooltip()
            }
            
            // Record 클릭 이벤트 처리
            manager.onRecordClick = { mapRecord ->
                Log.d("ClusterManagerInitializer", "🎯 Record 클릭: ${mapRecord.recordUrl}")
                try {
                    // Record 마커 선택 상태 업데이트
                    manager.selectRecordMarker(mapRecord)
                    
                    // 오디오 플레이어 실행
                    mapViewModel.playRecord(mapRecord)
                } catch (e: Exception) {
                    Log.e("ClusterManagerInitializer", "❌ [ERROR] Record 재생 중 예외 발생", e)
                }
            }
            
            // Record 클러스터 클릭 이벤트 처리
            manager.onRecordClusterClick = { clusterRecords ->
                Log.d("ClusterManagerInitializer", "🎯 Record 클러스터 클릭: ${clusterRecords.size}개 녹음")
                // Record 클러스터 클릭 시 목록 표시 등의 로직 추가 가능
            }
            
            Log.d("ClusterManagerInitializer", "ClusterManager 생성됨 - 툴팁 및 Record 콜백 연결됨")
        }
    }
}