package com.shinhan.campung.presentation.ui.map

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.model.MapRecord
import com.shinhan.campung.data.model.MapItem
import com.shinhan.campung.data.model.MapContentItem
import com.shinhan.campung.data.model.MapRecordItem
import com.shinhan.campung.data.model.createMixedMapItems
import com.shinhan.campung.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.*

class MapClusterManager(
    private val context: Context,
    val naverMap: NaverMap,
    private val mapContainer: ViewGroup? = null // 지도를 포함하는 컨테이너 뷰
) : MarkerIconProvider {

    // 마커 클릭 콜백
    var onMarkerClick: ((MapContent) -> Unit)? = null
    var onRecordClick: ((MapRecord) -> Unit)? = null
    var onClusterClick: ((List<MapContent>) -> Unit)? = null
    var onRecordClusterClick: ((List<MapRecord>) -> Unit)? = null
    var onCenterMarkerChanged: ((MapContent?) -> Unit)? = null
    
    // 통합 클러스터 콜백 (새로 추가)
    var onMixedClusterClick: ((List<MapItem>) -> Unit)? = null
    
    // 툴팁 콜백 (InfoWindow 대신 사용)
    var onShowTooltip: ((MapContent, com.shinhan.campung.presentation.ui.components.TooltipType) -> Unit)? = null
    var onHideTooltip: (() -> Unit)? = null
    
    // POI 충돌 방지를 위한 마커 위치 업데이트 콜백
    var onMarkerPositionsUpdated: ((List<LatLng>, Double) -> Unit)? = null

    // 선택된 마커 상태 관리
    var selectedMarker: Marker? = null
        private set
    var selectedContent: MapContent? = null
        private set
        
    // 선택된 Record 마커 상태 관리
    var selectedRecordMarker: Marker? = null
        private set
    var selectedRecord: MapRecord? = null
        private set
        
    // 클러스터 클릭으로 인한 카메라 이동 플래그
    var isClusterMoving = false
        private set
        
    // 선택된 클러스터 마커 상태 관리
    var selectedClusterMarker: Marker? = null
        private set


    private val markers = mutableListOf<Marker>()
    private val recordMarkers = mutableListOf<Marker>()
    private val clusterMarkers = mutableListOf<Marker>()
    private val recordClusterMarkers = mutableListOf<Marker>()
    private var quadTree: QuadTree? = null
    private var recordQuadTree: RecordQuadTree? = null
    private var lastMapContents: List<MapContent> = emptyList() // QuadTree 재사용을 위한 캐시
    private var lastMapRecords: List<MapRecord> = emptyList() // Record QuadTree 캐시
    
    // 마커 크기는 MarkerConfig에서 중앙 관리
    companion object {
        private val MARKER_SIZE get() = MarkerConfig.BASE_MARKER_SIZE
        private val SELECTED_MARKER_SCALE get() = MarkerConfig.SELECTED_SCALE
        private val HIGHLIGHTED_MARKER_SCALE get() = MarkerConfig.HIGHLIGHTED_SCALE
    }
    
    // 아이콘 캐싱 시스템
    private val normalIconCache = mutableMapOf<String, OverlayImage>()
    private val selectedIconCache = mutableMapOf<String, OverlayImage>()
    private val highlightedIconCache = mutableMapOf<String, OverlayImage>()
    private val clusterIconCache = mutableMapOf<String, OverlayImage>()
    
    // 렌더링 최적화 시스템
    private val markerPool = MarkerPool()
    private val renderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val markerRenderer = MarkerRenderer(naverMap, markerPool, renderScope)
    private val bitmapFactory = OptimizedBitmapFactory.getInstance(context)

    // InfoWindow 관련 코드 제거됨 - 이제 Compose 툴팁 사용

    fun setupClustering() {
        // 아이콘 캐시 초기화 - 모든 타입별로 미리 생성
        initializeIconCache()
        Log.d("MapClusterManager", "클러스터 관리자 설정 완료 - Compose 툴팁 사용, 아이콘 캐시 초기화됨")
    }
    
    private fun initializeIconCache() {
        val postTypes = listOf("NOTICE", "INFO", "MARKET", "FREE", "HOT", null) // null은 기본값
        
        postTypes.forEach { postType ->
            val key = postType ?: "DEFAULT"
            
            // 각 타입별로 3가지 상태의 아이콘 미리 생성
            normalIconCache[key] = createNormalMarkerIconInternal(postType)
            selectedIconCache[key] = createSelectedMarkerIconInternal(postType)
            highlightedIconCache[key] = createHighlightedMarkerIconInternal(postType)
        }
        
        // Record 마커 아이콘 초기화
        normalIconCache["marker_record"] = createRecordMarkerIconInternal(false)
        selectedIconCache["marker_record"] = createRecordMarkerIconInternal(true)
        
        // 클러스터 아이콘도 자주 사용되는 크기들 미리 생성
        val commonClusterSizes = listOf(2, 3, 4, 5, 10, 20, 50, 100)
        commonClusterSizes.forEach { size ->
            clusterIconCache["normal_$size"] = createClusterIconInternal(size, false)
            clusterIconCache["selected_$size"] = createClusterIconInternal(size, true)
        }
        
        Log.d("MapClusterManager", "아이콘 캐시 초기화 완료 - Normal: ${normalIconCache.size}, Selected: ${selectedIconCache.size}, Highlighted: ${highlightedIconCache.size}, Cluster: ${clusterIconCache.size}")
        Log.d("MapClusterManager", "Record 마커 아이콘도 초기화됨")
    }

    // 마커 선택 함수
    fun selectMarker(content: MapContent) {
        Log.d("MapClusterManager", "selectMarker 호출: ${content.title}")
        
        // 이전 선택 해제
        clearSelection()

        // 새로운 마커 찾기 및 선택
        val targetMarker = markers.find { 
            val markerContent = it.tag as? MapContent
            markerContent?.contentId == content.contentId
        }
        
        if (targetMarker != null) {
            selectedMarker = targetMarker
            selectedContent = content
            targetMarker.zIndex = 2000 // 최상위

            // 애니메이션과 함께 아이콘 변경
            animateMarkerSelection(targetMarker, true)

            Log.d("MapClusterManager", "마커 선택됨: ${content.title}")

            // 마커 애니메이션 후에 Compose 툴팁 표시 (약간의 딜레이)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onShowTooltip?.invoke(content, com.shinhan.campung.presentation.ui.components.TooltipType.CLICK)
            }, 150) // 마커 애니메이션이 어느정도 진행된 후에 툴팁 표시
        } else {
            Log.e("MapClusterManager", "마커를 찾을 수 없음: ${content.title}")
        }
    }

    // Record 마커 선택
    fun selectRecordMarker(record: MapRecord) {
        Log.d("MapClusterManager", "🎵 selectRecordMarker 호출: ${record.recordUrl}")
        
        // 이전 Content 마커 선택 해제
        selectedMarker?.let { marker ->
            animateMarkerSelection(marker, false)
            marker.zIndex = 50
        }
        selectedMarker = null
        selectedContent = null
        
        // 이전 Record 마커 선택 해제
        selectedRecordMarker?.let { marker ->
            animateRecordMarkerSelection(marker, false)
            marker.zIndex = 50
        }
        
        // 새로운 Record 마커 찾기 및 선택
        val targetRecordMarker = recordMarkers.find { 
            val markerRecord = it.tag as? MapRecord
            markerRecord?.recordId == record.recordId
        }
        
        if (targetRecordMarker != null) {
            selectedRecordMarker = targetRecordMarker
            selectedRecord = record
            targetRecordMarker.zIndex = 2000 // 최상위
            
            // 애니메이션과 함께 아이콘 변경
            animateRecordMarkerSelection(targetRecordMarker, true)
            
            Log.d("MapClusterManager", "Record 마커 선택됨: ${record.recordUrl}")
        } else {
            Log.e("MapClusterManager", "Record 마커를 찾을 수 없음: ${record.recordUrl}")
        }
    }

    // 선택 해제
    fun clearSelection() {
        Log.d("MapClusterManager", "🔄 clearSelection() 호출됨")
        Log.d("MapClusterManager", "selectedMarker: ${selectedMarker != null}")
        Log.d("MapClusterManager", "selectedRecordMarker: ${selectedRecordMarker != null}")
        Log.d("MapClusterManager", "selectedClusterMarker: ${selectedClusterMarker != null}")
        
        selectedMarker?.let { marker ->
            Log.d("MapClusterManager", "개별 마커 선택 해제")
            // 애니메이션과 함께 아이콘 변경
            animateMarkerSelection(marker, false)
            marker.zIndex = 50
        }
        selectedMarker = null
        selectedContent = null

        // 선택된 Record 마커도 해제
        selectedRecordMarker?.let { marker ->
            Log.d("MapClusterManager", "Record 마커 선택 해제")
            animateRecordMarkerSelection(marker, false)
            marker.zIndex = 50
        }
        selectedRecordMarker = null
        selectedRecord = null

        // 선택된 클러스터도 해제
        selectedClusterMarker?.let { clusterMarker ->
            Log.d("MapClusterManager", "클러스터 마커 선택 해제")
            
            // tag에서 실제 아이템 개수 가져오기 (더 정확한 방식)
            val count = when (val tag = clusterMarker.tag) {
                is List<*> -> tag.size // 클러스터 아이템 리스트
                is Int -> tag // 직접 저장된 개수
                else -> {
                    // captionText에서 파싱 시도 (폴백)
                    val captionText = clusterMarker.captionText
                    Log.d("MapClusterManager", "클러스터 captionText: '$captionText'")
                    when {
                        captionText.contains("개 항목") -> captionText.replace("개 항목", "").toIntOrNull() ?: 1
                        captionText.contains("개 게시글") -> captionText.replace("개 게시글", "").toIntOrNull() ?: 1
                        captionText.contains("개 녹음") -> captionText.replace("개 녹음", "").toIntOrNull() ?: 1
                        captionText.contains("개 (") -> {
                            // "5개 (게시글 3, 녹음 2)" 형식 파싱
                            captionText.substringBefore("개").toIntOrNull() ?: 1
                        }
                        else -> {
                            Log.w("MapClusterManager", "알 수 없는 captionText 형식: '$captionText' - 기본값 1 사용")
                            1
                        }
                    }
                }
            }
            
            Log.d("MapClusterManager", "클러스터 마커 선택 해제: ${count}개 아이템")
            clusterMarker.icon = getClusterIconInternal(count, false)
            clusterMarker.zIndex = 0
        }
        selectedClusterMarker = null

        // 하이라이트된 마커도 함께 해제
        highlightedMarker?.let { marker ->
            Log.d("MapClusterManager", "하이라이트 마커 해제")
            animateMarkerFocus(marker, false)
            marker.zIndex = 50
        }
        highlightedMarker = null

        // Compose 툴팁 숨김
        onHideTooltip?.invoke()
        Log.d("MapClusterManager", "✅ 마커, Record, 클러스터 선택 해제 완료됨")
    }

    // InfoWindow 관련 함수들 제거됨 - 이제 Compose 툴팁으로 대체됨

    fun findCenterMarker(): MapContent? {
        // 선택된 마커가 있으면 우선적으로 처리
        if (selectedMarker != null) {
            return selectedContent
        }

        val center = naverMap.cameraPosition.target
        var closestMarker: Marker? = null
        var minDistance = Double.MAX_VALUE

        Log.d("MapClusterManager", "findCenterMarker 호출됨: 마커 개수=${markers.size}")

        // 모든 마커에서 중앙에 가장 가까운 것 찾기
        markers.forEach { marker ->
            val distance = calculateDistance(
                center.latitude, center.longitude,
                marker.position.latitude, marker.position.longitude
            )
            if (distance < minDistance) {
                minDistance = distance
                closestMarker = marker
            }
        }

        // 하이라이트 업데이트 (선택된 마커가 없을 때만)
        updateHighlightedMarker(closestMarker)

        return closestMarker?.tag as? MapContent
    }

    private fun updateHighlightedMarker(newMarker: Marker?) {
        // 선택된 마커가 있거나 애니메이션 진행 중이면 하이라이트 변경 안함
        if (selectedMarker != null || isAnimating) return

        // 이전 하이라이트가 새로운 마커와 같으면 중복 처리 방지
        if (highlightedMarker == newMarker) return

        // 이전 하이라이트 제거 (툴팁도 함께)
        highlightedMarker?.let { marker ->
            if (marker != selectedMarker) { // 선택된 마커가 아닐 때만
                animateMarkerFocus(marker, false)
                marker.zIndex = 50
                // 이전 포커스 마커의 Compose 툴팁 숨김
                onHideTooltip?.invoke()
            }
        }

        // 새로운 하이라이트 적용
        highlightedMarker = newMarker
        val content = newMarker?.tag as? MapContent

        newMarker?.let { marker ->
            if (marker != selectedMarker) { // 선택된 마커가 아닐 때만
                animateMarkerFocus(marker, true)
                marker.zIndex = 1000
                
                // 포커스 툴팁은 MapSelectionManager에서 처리하도록 위임
                // (클러스터 선택 상태에서는 표시하지 않음)
            }
        }

        // 콜백 호출
        onCenterMarkerChanged?.invoke(content)
    }

    private var highlightedMarker: Marker? = null // 접근성을 위해 private으로 변경
    private var isAnimating = false // 애니메이션 진행 중 플래그

    fun updateMarkers(mapContents: List<MapContent>, mapRecords: List<MapRecord> = emptyList(), onComplete: (() -> Unit)? = null) {
        Log.d("MapClusterManager", "🔄 updateMarkers 호출 - Contents: ${mapContents.size}, Records: ${mapRecords.size}")
        
        // 대량 마커 처리시 점진적 렌더링 사용
        if (mapContents.size + mapRecords.size > 100) {
            updateMarkersProgressive(mapContents, mapRecords, onComplete)
        } else {
            updateMarkersLegacy(mapContents, mapRecords, onComplete)
        }
    }
    
    /**
     * 점진적 마커 업데이트 (대량 마커 최적화)
     */
    private fun updateMarkersProgressive(mapContents: List<MapContent>, mapRecords: List<MapRecord>, onComplete: (() -> Unit)? = null) {
        // 선택된 마커 정보 백업
        val wasSelectedContent = selectedContent
        val wasSelectedRecord = selectedRecord

        clearAllMarkers()

        Log.d("MapClusterManager", "🚀 점진적 마커 렌더링 시작 - Content: ${mapContents.size}, Record: ${mapRecords.size}")

        // 비동기 점진적 렌더링
        kotlinx.coroutines.MainScope().launch {
            try {
                markerRenderer.renderMarkersWithPriority(
                    contents = mapContents,
                    records = mapRecords,
                    iconProvider = this@MapClusterManager,
                    onProgress = { current, total ->
                        Log.v("MapClusterManager", "렌더링 진행: $current/$total")
                    },
                    onComplete = {
                        Log.d("MapClusterManager", "✅ 점진적 렌더링 완료")
                        
                        // 선택 상태 복원
                        restoreMarkerSelection(wasSelectedContent, wasSelectedRecord, mapContents, mapRecords)
                        
                        onComplete?.invoke()
                    }
                )
            } catch (e: Exception) {
                Log.e("MapClusterManager", "점진적 렌더링 오류", e)
                onComplete?.invoke()
            }
        }
    }
    
    /**
     * 기존 마커 업데이트 방식 (소량 마커)
     */
    private fun updateMarkersLegacy(mapContents: List<MapContent>, mapRecords: List<MapRecord>, onComplete: (() -> Unit)? = null) {
        // 선택된 마커 정보 백업 (Content와 Record 모두)
        val wasSelectedContent = selectedContent
        val wasSelectedRecord = selectedRecord

        clearAllMarkers()

        val currentZoom = naverMap.cameraPosition.zoom
        Log.d("MapClusterManager", "현재 줌 레벨: $currentZoom, Content 마커: ${mapContents.size}개, Record 마커: ${mapRecords.size}개")

        // 통합 클러스터링 모드 또는 개별 처리 모드 선택
        if (onMixedClusterClick != null) {
            // 새로운 통합 클러스터링 모드
            Log.d("MapClusterManager", "통합 클러스터링 모드 (줌 레벨: $currentZoom)")
            showMixedClusters(mapContents, mapRecords)
        } else {
            // 기존 개별 처리 모드 (하위 호환성)
            Log.d("MapClusterManager", "개별 클러스터링 모드 - Content (줌 레벨: $currentZoom)")
            showClusteredMarkers(mapContents)
            
            // Record 마커들 표시
            if (mapRecords.isNotEmpty()) {
                Log.d("MapClusterManager", "개별 클러스터링 모드 - Records (줌 레벨: $currentZoom)")
                showClusteredRecords(mapRecords)
            }
        }

        // 선택 상태 복원
        restoreMarkerSelection(wasSelectedContent, wasSelectedRecord, mapContents, mapRecords)

        // POI 매니저에 현재 마커 위치들 전달 (충돌 방지용)
        notifyMarkerPositions()

        // 클러스터링 완료 콜백 호출
        onComplete?.invoke()
    }
    
    /**
     * 마커 선택 상태 복원
     */
    private fun restoreMarkerSelection(
        wasSelectedContent: MapContent?,
        wasSelectedRecord: MapRecord?, 
        mapContents: List<MapContent>,
        mapRecords: List<MapRecord>
    ) {
        // 이전에 선택된 Content 마커가 있었다면 다시 선택
        wasSelectedContent?.let { prevSelected ->
            val stillExists = mapContents.find { it.contentId == prevSelected.contentId }
            stillExists?.let { content ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    selectMarker(content)
                    Log.d("MapClusterManager", "Content 마커 선택 상태 복원: ${content.title}")
                }, 50) // 마커 생성 후 짧은 딜레이
            }
        }

        // 이전에 선택된 Record 마커가 있었다면 다시 선택
        wasSelectedRecord?.let { prevSelected ->
            val stillExists = mapRecords.find { it.recordId == prevSelected.recordId }
            stillExists?.let { record ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    selectRecordMarker(record)
                    Log.d("MapClusterManager", "Record 마커 선택 상태 복원: ${record.recordUrl}")
                }, 50) // 마커 생성 후 짧은 딜레이
            }
        }
    }

    private fun showIndividualMarkers(mapContents: List<MapContent>) {
        mapContents.forEach { content ->
            val marker = Marker().apply {
                position = LatLng(content.location.latitude, content.location.longitude)
                icon = getNormalMarkerIcon(content.postType)
                map = naverMap
                tag = content // MapContent 저장

                setOnClickListener {
                    // 이미 선택된 마커를 다시 클릭하면 선택 해제
                    if (selectedContent?.contentId == content.contentId) {
                        clearSelection()
                        onMarkerClick?.invoke(content) // 콜백은 여전히 호출
                    } else {
                        // 새로운 마커 선택 및 카메라 이동 (줌레벨 유지)
                        selectMarker(content)
                        
                        // 마커 클릭 이동 플래그 설정
                        isClusterMoving = true
                        
                        naverMap.moveCamera(
                            CameraUpdate.scrollTo(LatLng(content.location.latitude, content.location.longitude))
                                .animate(CameraAnimation.Easing)
                        )
                        
                        // 애니메이션 완료 후 플래그 해제 (1초 후)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isClusterMoving = false
                        }, 1000)
                        
                        onMarkerClick?.invoke(content)
                    }
                    true
                }
            }
            markers.add(marker)
        }
    }

    private fun showClusteredMarkers(mapContents: List<MapContent>) {
        val clusterDistance = getClusterDistance()
        val clusters = clusterMarkers(mapContents, clusterDistance)

        Log.e("MapClusterManager", "🎯🎯🎯 [MARKER] showClusteredMarkers 시작!!!")
        Log.d("MapClusterManager", "📊 [MARKER] 입력 데이터: ${mapContents.size}개 콘텐츠")
        Log.d("MapClusterManager", "📊 [MARKER] 줌: ${naverMap.cameraPosition.zoom}, 클러스터 거리: ${clusterDistance}m, 생성된 클러스터: ${clusters.size}개")
        Log.d("MapClusterManager", "🔍 [MARKER] onMarkerClick 콜백 존재: ${onMarkerClick != null}")

        clusters.forEachIndexed { index, cluster ->
            Log.d("MapClusterManager", "📊 [MARKER] 클러스터 [$index]: ${cluster.size}개 아이템")

            if (cluster.size == 1) {
                // 단일 마커
                val content = cluster[0]
                Log.d("MapClusterManager", "📍 [MARKER] 단일 마커 생성: ${content.title} (ID: ${content.contentId})")
                Log.d("MapClusterManager", "📍 [MARKER] 위치: (${content.location.latitude}, ${content.location.longitude})")
                Log.d("MapClusterManager", "🔗 [MARKER] 마커 생성 시점 onMarkerClick: ${onMarkerClick}")
                
                val marker = Marker().apply {
                    position = LatLng(content.location.latitude, content.location.longitude)
                    icon = getNormalMarkerIcon(content.postType)
                    map = naverMap
                    tag = content // MapContent 저장

                    setOnClickListener {
                        Log.e("MapClusterManager", "🎯🎯🎯 [CLICK] 마커 클릭됨!!!")
                        Log.d("MapClusterManager", "🎯 [CLICK] 클릭된 마커: ${content.title} (ID: ${content.contentId})")
                        Log.d("MapClusterManager", "🎯 [CLICK] onMarkerClick 콜백 존재: ${onMarkerClick != null}")
                        
                        // 이미 선택된 마커를 다시 클릭하면 선택 해제
                        if (selectedContent?.contentId == content.contentId) {
                            Log.d("MapClusterManager", "🎯 [CLICK] 이미 선택된 마커 - 선택 해제")
                            clearSelection()
                            onMarkerClick?.invoke(content)
                        } else {
                            Log.d("MapClusterManager", "🎯 [CLICK] 새 마커 선택 - selectMarker 호출")
                            // 새로운 마커 선택 및 카메라 이동 (줌레벨 유지)
                            selectMarker(content)
                            
                            // 마커 클릭 이동 플래그 설정
                            isClusterMoving = true
                            
                            naverMap.moveCamera(
                                CameraUpdate.scrollTo(LatLng(content.location.latitude, content.location.longitude))
                                    .animate(CameraAnimation.Easing)
                            )
                            
                            // 애니메이션 완료 후 플래그 해제 (1초 후)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                isClusterMoving = false
                            }, 1000)
                            
                            Log.d("MapClusterManager", "🎯 [CLICK] onMarkerClick 콜백 호출 시작")
                            onMarkerClick?.invoke(content)
                            Log.d("MapClusterManager", "🎯 [CLICK] onMarkerClick 콜백 호출 완료")
                        }
                        true
                    }
                }
                markers.add(marker)
                Log.d("MapClusterManager", "✅ [MARKER] 단일 마커 리스트에 추가 완료 - 총 마커 수: ${markers.size}")
                Log.d("MapClusterManager", "✅ [MARKER] 마커가 지도에 추가됨: ${marker.map != null}")
            } else {
                // 클러스터 마커
                val centerLat = cluster.map { it.location.latitude }.average()
                val centerLng = cluster.map { it.location.longitude }.average()

                val clusterMarker = Marker().apply {
                    position = LatLng(centerLat, centerLng)
                    captionText = "${cluster.size}개 항목"
                    icon = getClusterIconInternal(cluster.size, false)
                    map = naverMap
                    tag = cluster.size // 실제 아이템 개수 저장

                    setOnClickListener {
                        // 개별 마커 선택 해제
                        selectedMarker?.let { marker ->
                            animateMarkerSelection(marker, false)
                            marker.zIndex = 50
                        }
                        selectedMarker = null
                        selectedContent = null

                        // 이전 선택된 클러스터 해제
                        selectedClusterMarker?.let { oldCluster ->
                            val oldCount = oldCluster.captionText.replace("개 항목", "").toIntOrNull() ?: 1
                            oldCluster.icon = getClusterIconInternal(oldCount, false)
                            oldCluster.zIndex = 0
                        }
                        
                        // 새로운 클러스터 선택
                        selectedClusterMarker = this
                        this.icon = getClusterIconInternal(cluster.size, true)
                        this.zIndex = 2000

                        // 클러스터 클릭 콜백 먼저 호출
                        onClusterClick?.invoke(cluster)

                        // 클러스터 이동 플래그 설정
                        isClusterMoving = true
                        
                        // 줌 레벨 유지하면서 애니메이션으로 중앙 이동
                        naverMap.moveCamera(
                            CameraUpdate.scrollTo(position)
                                .animate(CameraAnimation.Easing)
                        )
                        
                        // 애니메이션 완료 후 플래그 해제 (1초 후)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isClusterMoving = false
                        }, 1000)
                        
                        true
                    }
                }
                clusterMarkers.add(clusterMarker)
            }
        }
    }

    private fun clusterMarkers(mapContents: List<MapContent>, distance: Double): List<List<MapContent>> {
        // QuadTree 재사용 최적화: 데이터가 변경된 경우에만 재생성
        if (quadTree == null || !isSameMapContents(mapContents)) {
            Log.d("MapClusterManager", "QuadTree 생성/갱신: ${mapContents.size}개 항목")
            quadTree = QuadTree.fromMapContents(mapContents)
            lastMapContents = mapContents.toList() // 복사본 저장
        } else {
            Log.d("MapClusterManager", "QuadTree 재사용: ${mapContents.size}개 항목")
        }
        
        val clusters = mutableListOf<MutableList<MapContent>>()
        val processed = mutableSetOf<MapContent>()

        mapContents.forEach { content ->
            if (content in processed) return@forEach

            val cluster = mutableListOf<MapContent>()
            cluster.add(content)
            processed.add(content)

            // QuadTree를 사용해서 반경 내의 마커들만 검색 (O(log n))
            val nearbyContents = quadTree?.queryRadius(
                content.location.latitude, 
                content.location.longitude, 
                distance
            ) ?: emptyList()

            nearbyContents.forEach { other ->
                if (other != content && other !in processed) {
                    // QuadTree에서 이미 거리 필터링이 되었으므로 바로 클러스터에 추가
                    cluster.add(other)
                    processed.add(other)
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }
    
    /**
     * 맵 데이터가 이전과 동일한지 확인 (QuadTree 재사용 최적화)
     */
    private fun isSameMapContents(newContents: List<MapContent>): Boolean {
        if (lastMapContents.size != newContents.size) return false
        
        // contentId 기준으로 빠른 비교
        val lastIds = lastMapContents.map { it.contentId }.toSet()
        val newIds = newContents.map { it.contentId }.toSet()
        
        return lastIds == newIds
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun getClusterDistance(): Double {
        return when {
            naverMap.cameraPosition.zoom >= 21 -> 2.0    // 2m - 매우 촘촘하게
            naverMap.cameraPosition.zoom >= 20 -> 3.0    // 3m - 초세밀
            naverMap.cameraPosition.zoom >= 19 -> 5.0    // 5m - 세밀  
            naverMap.cameraPosition.zoom >= 18 -> 8.0    // 8m - 매우 세밀
            naverMap.cameraPosition.zoom >= 17 -> 12.0   // 12m - 세밀
            naverMap.cameraPosition.zoom >= 16 -> 18.0   // 18m
            naverMap.cameraPosition.zoom >= 15 -> 8.0    // 8m - 더 빨리 나뉘게
            naverMap.cameraPosition.zoom >= 14 -> 12.0   // 12m
            naverMap.cameraPosition.zoom >= 13 -> 18.0   // 18m
            naverMap.cameraPosition.zoom >= 12 -> 30.0   // 30m
            naverMap.cameraPosition.zoom >= 11 -> 60.0   // 60m
            naverMap.cameraPosition.zoom >= 10 -> 120.0  // 120m
            else -> 250.0 // 250m - 멀리서도 적당히
        }
    }

    // Record 전용 클러스터링 거리 - Content보다 조금 더 촘촘하게
    private fun getRecordClusterDistance(): Double {
        return when {
            naverMap.cameraPosition.zoom >= 21 -> 1.5    // 1.5m - 매우 촘촘하게
            naverMap.cameraPosition.zoom >= 20 -> 2.5    // 2.5m - 초세밀  
            naverMap.cameraPosition.zoom >= 19 -> 4.0    // 4m - 세밀
            naverMap.cameraPosition.zoom >= 18 -> 6.0    // 6m - 매우 세밀
            naverMap.cameraPosition.zoom >= 17 -> 9.0    // 9m - 세밀
            naverMap.cameraPosition.zoom >= 16 -> 14.0   // 14m
            naverMap.cameraPosition.zoom >= 15 -> 6.0    // 6m - 더 빨리 나뉘게
            naverMap.cameraPosition.zoom >= 14 -> 9.0    // 9m
            naverMap.cameraPosition.zoom >= 13 -> 14.0   // 14m
            naverMap.cameraPosition.zoom >= 12 -> 25.0   // 25m
            naverMap.cameraPosition.zoom >= 11 -> 50.0   // 50m
            naverMap.cameraPosition.zoom >= 10 -> 100.0  // 100m
            else -> 200.0 // 200m - 멀리서도 적당히
        }
    }

    private fun clearAllMarkers() {
        Log.d("MapClusterManager", "🧹 clearAllMarkers 시작 - markers: ${markers.size}, records: ${recordMarkers.size}, clusters: ${clusterMarkers.size}, recordClusters: ${recordClusterMarkers.size}")
        Log.d("MapClusterManager", "🔗 [CLEAR] clearAllMarkers 호출 전 onMarkerClick: ${onMarkerClick}")
        
        // 각 마커를 지도에서 정리 (지도에서만 제거, 클릭 리스너는 유지)
        markers.forEach { marker ->
            marker.map = null
            marker.tag = null  // 태그는 제거해도 됨
        }
        recordMarkers.forEach { marker ->
            marker.map = null
            marker.tag = null
        }
        clusterMarkers.forEach { marker ->
            marker.map = null
            marker.tag = null
        }
        recordClusterMarkers.forEach { marker ->
            marker.map = null
            marker.tag = null
        }

        markers.clear()
        recordMarkers.clear()
        clusterMarkers.clear()
        recordClusterMarkers.clear()

        // 선택 상태만 초기화 (콜백은 유지)
        selectedMarker = null
        selectedContent = null
        selectedRecordMarker = null 
        selectedRecord = null
        selectedClusterMarker = null
        highlightedMarker = null
        
        // 클러스터링 상태도 초기화
        isClusterMoving = false
        
        Log.d("MapClusterManager", "✅ clearAllMarkers 완료 - 마커 정리됨, 콜백 유지됨")
        Log.d("MapClusterManager", "🔗 [CLEAR] clearAllMarkers 호출 후 onMarkerClick: ${onMarkerClick}")
    }

    fun clearMarkers() {
        clearAllMarkers()
        clearSelection()
    }
    
    /**
     * 리소스 정리 (메모리 누수 방지) - 앱 종료 시만 사용
     */
    fun cleanup() {
        Log.d("MapClusterManager", "MapClusterManager 완전 정리 시작")
        
        // 렌더링 작업 취소
        markerRenderer.cleanup()
        
        // 모든 마커를 완전히 정리 (리스너 포함)
        markers.forEach { marker ->
            marker.map = null
            marker.onClickListener = null  // cleanup 시에만 리스너 제거
            marker.tag = null
        }
        recordMarkers.forEach { marker ->
            marker.map = null
            marker.onClickListener = null
            marker.tag = null
        }
        clusterMarkers.forEach { marker ->
            marker.map = null
            marker.onClickListener = null
            marker.tag = null
        }
        recordClusterMarkers.forEach { marker ->
            marker.map = null
            marker.onClickListener = null
            marker.tag = null
        }
        
        markers.clear()
        recordMarkers.clear()
        clusterMarkers.clear()
        recordClusterMarkers.clear()
        
        // 마커 풀 정리
        markerPool.cleanup()
        
        // 비트맵 팩토리 정리
        bitmapFactory.cleanup()
        
        // 아이콘 캐시 정리
        normalIconCache.clear()
        selectedIconCache.clear() 
        highlightedIconCache.clear()
        clusterIconCache.clear()
        
        // QuadTree 정리
        quadTree = null
        recordQuadTree = null
        
        // 콜백 정리 (완전 종료 시에만)
        onMarkerClick = null
        onRecordClick = null
        onClusterClick = null
        onRecordClusterClick = null
        onMixedClusterClick = null
        onCenterMarkerChanged = null
        onShowTooltip = null
        onHideTooltip = null
        
        Log.d("MapClusterManager", "MapClusterManager 완전 정리 완료")
    }

    /**
     * Content와 Record를 통합해서 클러스터링하는 새로운 함수
     */
    private fun showMixedClusters(mapContents: List<MapContent>, mapRecords: List<MapRecord>) {
        // MapItem으로 변환하고 통합
        val mixedItems = createMixedMapItems(mapContents, mapRecords)
        val clusterDistance = getClusterDistance()
        val clusters = clusterMixedItems(mixedItems, clusterDistance)

        Log.e("MapClusterManager", "🎯🎯🎯 [MIXED] showMixedClusters 시작!!!")
        Log.d("MapClusterManager", "📊 [MIXED] 입력 데이터: ${mapContents.size}개 Content + ${mapRecords.size}개 Record = ${mixedItems.size}개 총합")
        Log.d("MapClusterManager", "📊 [MIXED] 줌: ${naverMap.cameraPosition.zoom}, 클러스터 거리: ${clusterDistance}m, 생성된 클러스터: ${clusters.size}개")

        clusters.forEachIndexed { index, cluster ->
            Log.d("MapClusterManager", "📊 [MIXED] 클러스터 [$index]: ${cluster.size}개 아이템")

            if (cluster.size == 1) {
                // 단일 마커
                val item = cluster[0]
                Log.d("MapClusterManager", "📍 [MIXED] 단일 마커 생성: ${item.title} (ID: ${item.id}, 타입: ${item.type})")
                
                when (item) {
                    is MapContentItem -> {
                        val marker = createContentMarker(item.content)
                        markers.add(marker)
                        Log.d("MapClusterManager", "✅ [MIXED] Content 마커 추가 완료")
                    }
                    is MapRecordItem -> {
                        val marker = createRecordMarker(item.record)
                        recordMarkers.add(marker)
                        Log.d("MapClusterManager", "✅ [MIXED] Record 마커 추가 완료")
                    }
                }
            } else {
                // 통합 클러스터 마커
                val centerLat = cluster.map { it.location.latitude }.average()
                val centerLng = cluster.map { it.location.longitude }.average()

                // 클러스터 구성 분석
                val contentCount = cluster.count { it.type == com.shinhan.campung.data.model.MapItemType.CONTENT }
                val recordCount = cluster.count { it.type == com.shinhan.campung.data.model.MapItemType.RECORD }
                
                val clusterText = when {
                    contentCount > 0 && recordCount > 0 -> "${cluster.size}개 (게시글 ${contentCount}, 녹음 ${recordCount})"
                    contentCount > 0 -> "${cluster.size}개 게시글"
                    else -> "${cluster.size}개 녹음"
                }

                val clusterMarker = Marker().apply {
                    position = LatLng(centerLat, centerLng)
                    captionText = clusterText
                    icon = createMixedClusterIcon(cluster.size, contentCount, recordCount, false)
                    map = naverMap
                    tag = cluster.size // 실제 아이템 개수 저장

                    setOnClickListener {
                        Log.e("MapClusterManager", "🎯🎯🎯 [MIXED CLUSTER] 통합 클러스터 클릭!!!")
                        Log.d("MapClusterManager", "🎯 [MIXED CLUSTER] 클릭된 클러스터: ${cluster.size}개 아이템 (Content: ${contentCount}, Record: ${recordCount})")
                        
                        // 개별 마커 선택 해제
                        selectedMarker?.let { marker ->
                            animateMarkerSelection(marker, false)
                            marker.zIndex = 0
                        }
                        selectedMarker = null
                        selectedContent = null
                        
                        selectedRecordMarker?.let { marker ->
                            animateRecordMarkerSelection(marker, false)
                            marker.zIndex = 0
                        }
                        selectedRecordMarker = null
                        selectedRecord = null

                        // 이전 선택된 클러스터 해제
                        selectedClusterMarker?.let { oldCluster ->
                            // 이전 클러스터 아이콘 복원 (믹스드 클러스터인지 확인 필요)
                            oldCluster.icon = createMixedClusterIcon(cluster.size, contentCount, recordCount, false)
                            oldCluster.zIndex = 0
                        }
                        
                        // 새로운 클러스터 선택
                        selectedClusterMarker = this
                        this.icon = createMixedClusterIcon(cluster.size, contentCount, recordCount, true)
                        this.zIndex = 2000

                        // 통합 클러스터 클릭 콜백 호출
                        onMixedClusterClick?.invoke(cluster)

                        // 클러스터 이동 플래그 설정
                        isClusterMoving = true
                        
                        // 줌 레벨 유지하면서 애니메이션으로 중앙 이동
                        naverMap.moveCamera(
                            CameraUpdate.scrollTo(position)
                                .animate(CameraAnimation.Easing)
                        )
                        
                        // 애니메이션 완료 후 플래그 해제 (1초 후)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isClusterMoving = false
                        }, 1000)
                        
                        true
                    }
                }
                clusterMarkers.add(clusterMarker)
                Log.d("MapClusterManager", "✅ [MIXED] 통합 클러스터 마커 추가 완료")
            }
        }
    }

    /**
     * Content 마커를 생성하는 헬퍼 함수
     */
    private fun createContentMarker(content: MapContent): Marker {
        return Marker().apply {
            position = LatLng(content.location.latitude, content.location.longitude)
            icon = getNormalMarkerIcon(content.postType)
            map = naverMap
            tag = content

            setOnClickListener {
                Log.e("MapClusterManager", "🎯🎯🎯 [MIXED CONTENT] 개별 Content 마커 클릭!!!")
                
                if (selectedContent?.contentId == content.contentId) {
                    clearSelection()
                    onMarkerClick?.invoke(content)
                } else {
                    selectMarker(content)
                    
                    isClusterMoving = true
                    
                    naverMap.moveCamera(
                        CameraUpdate.scrollTo(LatLng(content.location.latitude, content.location.longitude))
                            .animate(CameraAnimation.Easing)
                    )
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isClusterMoving = false
                    }, 1000)
                    
                    onMarkerClick?.invoke(content)
                }
                true
            }
        }
    }

    /**
     * Record 마커를 생성하는 헬퍼 함수
     */
    private fun createRecordMarker(record: MapRecord): Marker {
        return Marker().apply {
            position = LatLng(record.location.latitude, record.location.longitude)
            icon = getRecordMarkerIcon(false)
            map = naverMap
            tag = record

            setOnClickListener {
                Log.e("MapClusterManager", "🎯🎯🎯 [MIXED RECORD] 개별 Record 마커 클릭!!!")
                onRecordClick?.invoke(record)
                true
            }
        }
    }

    /**
     * 통합 클러스터 아이콘 생성
     */
    private fun createMixedClusterIcon(totalCount: Int, contentCount: Int, recordCount: Int, isSelected: Boolean): OverlayImage {
        val key = "mixed_${totalCount}_${contentCount}_${recordCount}_${if (isSelected) "selected" else "normal"}"
        return clusterIconCache[key] ?: createMixedClusterIconInternal(totalCount, contentCount, recordCount, isSelected).also {
            clusterIconCache[key] = it
        }
    }

    private fun createMixedClusterIconInternal(totalCount: Int, contentCount: Int, recordCount: Int, isSelected: Boolean): OverlayImage {
        val size = if (isSelected) MarkerConfig.CLUSTER_SELECTED_SIZE else MarkerConfig.CLUSTER_BASE_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 배경 원 그리기 (혼합 클러스터 전용 색상)
        val paint = Paint().apply {
            isAntiAlias = true
            color = if (isSelected) Color.parseColor("#FF673AB7") else Color.parseColor("#FF9C27B0") // 보라색 계열 (혼합 표시)
            style = Paint.Style.FILL
        }

        val radius = size / 2f - 2f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // 테두리 그리기
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = if (isSelected) 6f else 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // 선택 시 추가 외곽 테두리
        if (isSelected) {
            paint.apply {
                color = Color.parseColor("#FFFF9800") // 오렌지색 외곽 테두리
                strokeWidth = 2f
            }
            canvas.drawCircle(size / 2f, size / 2f, radius + 4f, paint)
        }

        // 텍스트 그리기 (간단하게 총 개수만)
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = when {
                totalCount < 10 -> if (isSelected) 28f else 24f
                totalCount < 100 -> if (isSelected) 24f else 20f
                else -> if (isSelected) 20f else 16f
            }
        }

        val text = if (totalCount > 999) "999+" else totalCount.toString()
        val textY = size / 2f + paint.textSize / 3f
        canvas.drawText(text, size / 2f, textY, paint)

        return OverlayImage.fromBitmap(bitmap)
    }

    /**
     * MapItem들을 클러스터링하는 함수
     */
    private fun clusterMixedItems(mixedItems: List<MapItem>, distance: Double): List<List<MapItem>> {
        val clusters = mutableListOf<MutableList<MapItem>>()
        val processed = mutableSetOf<MapItem>()

        mixedItems.forEach { item ->
            if (item in processed) return@forEach

            val cluster = mutableListOf<MapItem>()
            cluster.add(item)
            processed.add(item)

            // 반경 내의 다른 아이템들 검색
            mixedItems.forEach { other ->
                if (other != item && other !in processed) {
                    val itemDistance = calculateDistance(
                        item.location.latitude, item.location.longitude,
                        other.location.latitude, other.location.longitude
                    )
                    if (itemDistance <= distance) {
                        cluster.add(other)
                        processed.add(other)
                    }
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }

    private fun createSelectedMarkerIcon(postType: String? = null): OverlayImage {
        val drawableRes = when(postType) {
            "NOTICE" -> R.drawable.marker_notice
            "INFO" -> R.drawable.marker_info
            "MARKET" -> R.drawable.marker_market
            "FREE" -> R.drawable.marker_free
            "HOT" -> R.drawable.marker_hot
            else -> R.drawable.marker_info // 기본값
        }
        
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        val size = (MARKER_SIZE * SELECTED_MARKER_SCALE).toInt() // 선택 시 더 크게
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }

    // 마커 선택/해제 애니메이션 - 실제 크기 변화 애니메이션
    private fun animateMarkerSelection(marker: Marker, isSelected: Boolean) {
        val content = marker.tag as? MapContent
        
        if (isSelected) {
            // 선택 시: 1.0 → 1.5 크기로 부드럽게 애니메이션
            val scaleAnimator = ObjectAnimator.ofFloat(1.0f, 1.5f)
            scaleAnimator.duration = 300
            scaleAnimator.interpolator = android.view.animation.OvershootInterpolator(1.8f)
            
            scaleAnimator.addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                marker.icon = createIntermediateMarkerIcon(content?.postType, scale)
            }
            
            scaleAnimator.start()
            
        } else {
            // 해제 시: 현재 크기 → 1.0으로 부드럽게 축소
            val scaleAnimator = ObjectAnimator.ofFloat(1.5f, 1.0f)
            scaleAnimator.duration = 200
            scaleAnimator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            scaleAnimator.addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                marker.icon = createIntermediateMarkerIcon(content?.postType, scale)
            }
            
            scaleAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    marker.icon = getNormalMarkerIcon(content?.postType)
                }
            })
            
            scaleAnimator.start()
        }
    }

    // 마커 포커스 애니메이션 (중앙 근처 마커) - 부드러운 크기 변화
    private fun animateMarkerFocus(marker: Marker, isFocused: Boolean) {
        val content = marker.tag as? MapContent
        
        if (isFocused) {
            // 애니메이션 시작시 플래그 설정
            isAnimating = true
            
            // 포커스 시: 1.0 → 1.4 크기로 부드럽게 애니메이션
            val scaleAnimator = ObjectAnimator.ofFloat(1.0f, 1.4f)
            scaleAnimator.duration = 200
            scaleAnimator.interpolator = android.view.animation.DecelerateInterpolator()
            
            scaleAnimator.addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                marker.icon = createIntermediateMarkerIcon(content?.postType, scale)
            }
            
            scaleAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false // 애니메이션 완료시 플래그 해제
                }
            })
            
            scaleAnimator.start()
            
        } else {
            // 애니메이션 시작시 플래그 설정
            isAnimating = true
            
            // 포커스 해제 시: 1.4 → 1.0으로 부드럽게 축소
            val scaleAnimator = ObjectAnimator.ofFloat(1.4f, 1.0f)
            scaleAnimator.duration = 150
            scaleAnimator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            scaleAnimator.addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                marker.icon = createIntermediateMarkerIcon(content?.postType, scale)
            }
            
            scaleAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    marker.icon = getNormalMarkerIcon(content?.postType)
                    isAnimating = false // 애니메이션 완료시 플래그 해제
                }
            })
            
            scaleAnimator.start()
        }
    }

    private fun createClusterIcon(count: Int, isSelected: Boolean = false): OverlayImage {
        val size = if (isSelected) MarkerConfig.CLUSTER_SELECTED_SIZE else MarkerConfig.CLUSTER_BASE_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 배경 원 그리기
        val paint = Paint().apply {
            isAntiAlias = true
            color = if (isSelected) Color.parseColor("#FF1976D2") else Color.parseColor("#FF3F51B5") // 선택 시 더 진한 파랑
            style = Paint.Style.FILL
        }

        val radius = size / 2f - 2f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // 테두리 그리기
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = if (isSelected) 6f else 4f // 선택 시 테두리 두께 증가
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // 선택 시 추가 외곽 테두리
        if (isSelected) {
            paint.apply {
                color = Color.parseColor("#FFE91E63") // 핑크색 외곽 테두리
                strokeWidth = 2f
            }
            canvas.drawCircle(size / 2f, size / 2f, radius + 4f, paint)
        }

        // 텍스트 그리기
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = when {
                count < 10 -> if (isSelected) 28f else 24f
                count < 100 -> if (isSelected) 24f else 20f
                else -> if (isSelected) 20f else 16f
            }
        }

        val text = if (count > 999) "999+" else count.toString()
        val textY = size / 2f + paint.textSize / 3f
        canvas.drawText(text, size / 2f, textY, paint)

        return OverlayImage.fromBitmap(bitmap)
    }

    private fun createHighlightedMarkerIcon(postType: String? = null): OverlayImage {
        val drawableRes = when(postType) {
            "NOTICE" -> R.drawable.marker_notice
            "INFO" -> R.drawable.marker_info
            "MARKET" -> R.drawable.marker_market
            "FREE" -> R.drawable.marker_free
            "HOT" -> R.drawable.marker_hot
            else -> R.drawable.marker_info // 기본값
        }
        
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        val size = (MARKER_SIZE * HIGHLIGHTED_MARKER_SCALE).toInt() // 하이라이트 크기
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }

    private fun createNormalMarkerIcon(postType: String? = null): OverlayImage {
        val drawableRes = when(postType) {
            "NOTICE" -> R.drawable.marker_notice
            "INFO" -> R.drawable.marker_info
            "MARKET" -> R.drawable.marker_market
            "FREE" -> R.drawable.marker_free
            "HOT" -> R.drawable.marker_hot
            else -> R.drawable.marker_info // 기본값
        }
        
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        val size = MARKER_SIZE // 기본 마커 크기
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888) // 높이를 약간 더 크게
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }

    private fun createIntermediateMarkerIcon(postType: String? = null, scale: Float): OverlayImage {
        val drawableRes = when(postType) {
            "NOTICE" -> R.drawable.marker_notice
            "INFO" -> R.drawable.marker_info
            "MARKET" -> R.drawable.marker_market
            "FREE" -> R.drawable.marker_free
            "HOT" -> R.drawable.marker_hot
            else -> R.drawable.marker_info // 기본값
        }
        
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        val size = (MARKER_SIZE * scale).toInt() // 기본 크기에 스케일 적용
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }
    
    // MarkerIconProvider 인터페이스 구현
    override fun getNormalContentIcon(postType: String?): OverlayImage {
        return getNormalMarkerIcon(postType)
    }
    
    override fun getSelectedContentIcon(postType: String?): OverlayImage {
        return getSelectedMarkerIcon(postType)
    }
    
    override fun getHighlightedContentIcon(postType: String?): OverlayImage {
        return getHighlightedMarkerIcon(postType)
    }
    
    override fun getNormalRecordIcon(): OverlayImage {
        return getRecordMarkerIcon(false)
    }
    
    override fun getSelectedRecordIcon(): OverlayImage {
        return getRecordMarkerIcon(true)
    }
    
    override fun getClusterIcon(count: Int, isSelected: Boolean): OverlayImage {
        return getClusterIconInternal(count, isSelected)
    }

    // 캐시 접근 함수들 - 외부에서 사용
    private fun getNormalMarkerIcon(postType: String?): OverlayImage {
        val key = postType ?: "DEFAULT"
        return normalIconCache[key] ?: createNormalMarkerIconInternal(postType).also {
            normalIconCache[key] = it
        }
    }
    
    private fun getSelectedMarkerIcon(postType: String?): OverlayImage {
        val key = postType ?: "DEFAULT"
        return selectedIconCache[key] ?: createSelectedMarkerIconInternal(postType).also {
            selectedIconCache[key] = it
        }
    }
    
    private fun getHighlightedMarkerIcon(postType: String?): OverlayImage {
        val key = postType ?: "DEFAULT"
        return highlightedIconCache[key] ?: createHighlightedMarkerIconInternal(postType).also {
            highlightedIconCache[key] = it
        }
    }
    
    private fun getClusterIconInternal(count: Int, isSelected: Boolean): OverlayImage {
        val key = if (isSelected) "selected_$count" else "normal_$count"
        return clusterIconCache[key] ?: createClusterIconInternal(count, isSelected).also {
            clusterIconCache[key] = it
        }
    }

    // 내부 아이콘 생성 함수들 - 캐시 미스시에만 호출 (최적화된 버전)
    private fun createNormalMarkerIconInternal(postType: String?): OverlayImage {
        val bitmap = bitmapFactory.createMarkerBitmap(postType, scale = 1.0f)
        return OverlayImage.fromBitmap(bitmap)
    }

    private fun createSelectedMarkerIconInternal(postType: String?): OverlayImage {
        val bitmap = bitmapFactory.createMarkerBitmap(postType, scale = 1.5f)
        return OverlayImage.fromBitmap(bitmap)
    }

    private fun createHighlightedMarkerIconInternal(postType: String?): OverlayImage {
        val bitmap = bitmapFactory.createMarkerBitmap(postType, scale = 1.4f)
        return OverlayImage.fromBitmap(bitmap)
    }
    
    private fun createClusterIconInternal(count: Int, isSelected: Boolean): OverlayImage {
        val bitmap = bitmapFactory.createClusterBitmap(count, isSelected, useOptimizedPath = true)
        return OverlayImage.fromBitmap(bitmap)
    }
    
    private fun showClusteredRecords(mapRecords: List<MapRecord>) {
        val clusterDistance = getRecordClusterDistance() // Record 전용 거리 함수 사용
        val clusters = clusterRecords(mapRecords, clusterDistance)

        Log.d("MapClusterManager", "줌: ${naverMap.cameraPosition.zoom}, Record 클러스터 거리: ${clusterDistance}m, 생성된 클러스터: ${clusters.size}개")

        clusters.forEach { cluster ->
            if (cluster.size == 1) {
                // 단일 Record 마커
                val record = cluster[0]
                val marker = Marker().apply {
                    position = LatLng(record.location.latitude, record.location.longitude)
                    icon = getRecordMarkerIcon(false)
                    map = naverMap
                    tag = record // MapRecord 저장

                    setOnClickListener {
                        // Record 클릭 콜백
                        onRecordClick?.invoke(record)
                        true
                    }
                }
                recordMarkers.add(marker)
            } else {
                // Record 클러스터 마커
                val centerLat = cluster.map { it.location.latitude }.average()
                val centerLng = cluster.map { it.location.longitude }.average()

                val clusterMarker = Marker().apply {
                    position = LatLng(centerLat, centerLng)
                    captionText = "${cluster.size}개 녹음"
                    icon = getRecordClusterIcon(cluster.size, false)
                    map = naverMap
                    tag = cluster.size // 실제 아이템 개수 저장

                    setOnClickListener {
                        // Record 클러스터 클릭 콜백
                        onRecordClusterClick?.invoke(cluster)
                        
                        // 클러스터 이동 플래그 설정
                        isClusterMoving = true
                        
                        // 클러스터 크기에 따른 적절한 줌 레벨 계산 - 더 많이 확대
                        val currentZoom = naverMap.cameraPosition.zoom
                        val targetZoom = when {
                            cluster.size <= 2 -> minOf(currentZoom + 3.0, 19.0)  // 매우 작은 클러스터: 3레벨 확대
                            cluster.size <= 5 -> minOf(currentZoom + 2.5, 18.5)  // 작은 클러스터: 2.5레벨 확대
                            cluster.size <= 15 -> minOf(currentZoom + 2.0, 18.0) // 중간 클러스터: 2레벨 확대  
                            else -> minOf(currentZoom + 1.5, 17.5) // 큰 클러스터: 1.5레벨 확대
                        }
                        
                        Log.d("MapClusterManager", "Record 클러스터 확대: ${cluster.size}개 → 줌 $currentZoom → $targetZoom")
                        
                        // 중앙 이동과 함께 확대
                        naverMap.moveCamera(
                            CameraUpdate.scrollAndZoomTo(position, targetZoom)
                                .animate(CameraAnimation.Easing)
                        )
                        
                        // 애니메이션 완료 후 플래그 해제
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isClusterMoving = false
                        }, 1000)
                        
                        true
                    }
                }
                recordClusterMarkers.add(clusterMarker)
            }
        }
    }

    private fun clusterRecords(mapRecords: List<MapRecord>, distance: Double): List<List<MapRecord>> {
        // Record QuadTree 재사용 최적화
        if (recordQuadTree == null || !isSameMapRecords(mapRecords)) {
            Log.d("MapClusterManager", "Record QuadTree 생성/갱신: ${mapRecords.size}개 항목")
            recordQuadTree = RecordQuadTree.fromMapRecords(mapRecords)
            lastMapRecords = mapRecords.toList()
        } else {
            Log.d("MapClusterManager", "Record QuadTree 재사용: ${mapRecords.size}개 항목")
        }
        
        val clusters = mutableListOf<MutableList<MapRecord>>()
        val processed = mutableSetOf<MapRecord>()

        mapRecords.forEach { record ->
            if (record in processed) return@forEach

            val cluster = mutableListOf<MapRecord>()
            cluster.add(record)
            processed.add(record)

            // Record QuadTree를 사용해서 반경 내의 record들만 검색
            val nearbyRecords = recordQuadTree?.queryRadius(
                record.location.latitude, 
                record.location.longitude, 
                distance
            ) ?: emptyList()

            nearbyRecords.forEach { other ->
                if (other != record && other !in processed) {
                    cluster.add(other)
                    processed.add(other)
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }
    
    private fun isSameMapRecords(newRecords: List<MapRecord>): Boolean {
        if (lastMapRecords.size != newRecords.size) return false
        
        val lastIds = lastMapRecords.map { it.recordId }.toSet()
        val newIds = newRecords.map { it.recordId }.toSet()
        
        return lastIds == newIds
    }
    
    private fun getRecordMarkerIcon(isSelected: Boolean): OverlayImage {
        val key = "marker_record"
        return if (isSelected) {
            selectedIconCache[key] ?: createRecordMarkerIconInternal(true).also {
                selectedIconCache[key] = it
            }
        } else {
            normalIconCache[key] ?: createRecordMarkerIconInternal(false).also {
                normalIconCache[key] = it
            }
        }
    }
    
    private fun getRecordClusterIcon(count: Int, isSelected: Boolean): OverlayImage {
        val key = if (isSelected) "record_cluster_selected_$count" else "record_cluster_normal_$count"
        return clusterIconCache[key] ?: createRecordClusterIconInternal(count, isSelected).also {
            clusterIconCache[key] = it
        }
    }
    
    private fun createRecordMarkerIconInternal(isSelected: Boolean): OverlayImage {
        val drawable = ContextCompat.getDrawable(context, R.drawable.marker_record)
        val size = if (isSelected) MarkerConfig.RECORD_SELECTED_SIZE else MarkerConfig.RECORD_MARKER_SIZE
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }
    
    private fun createRecordClusterIconInternal(count: Int, isSelected: Boolean): OverlayImage {
        val size = if (isSelected) MarkerConfig.CLUSTER_SELECTED_SIZE else MarkerConfig.CLUSTER_BASE_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 배경 원 그리기 (녹음 전용 색상)
        val paint = Paint().apply {
            isAntiAlias = true
            color = if (isSelected) Color.parseColor("#FFD32F2F") else Color.parseColor("#FFF44336") // 빨간색 계열
            style = Paint.Style.FILL
        }

        val radius = size / 2f - 2f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // 테두리 그리기
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = if (isSelected) 6f else 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // 선택 시 추가 외곽 테두리
        if (isSelected) {
            paint.apply {
                color = Color.parseColor("#FFFF5722") // 오렌지색 외곽 테두리
                strokeWidth = 2f
            }
            canvas.drawCircle(size / 2f, size / 2f, radius + 4f, paint)
        }

        // 텍스트 그리기
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = when {
                count < 10 -> if (isSelected) 28f else 24f
                count < 100 -> if (isSelected) 24f else 20f
                else -> if (isSelected) 20f else 16f
            }
        }

        val text = if (count > 999) "999+" else count.toString()
        val textY = size / 2f + paint.textSize / 3f
        canvas.drawText(text, size / 2f, textY, paint)

        return OverlayImage.fromBitmap(bitmap)
    }
    
    // Record 마커 선택/해제 애니메이션
    private fun animateRecordMarkerSelection(marker: Marker, isSelected: Boolean) {
        val record = marker.tag as? MapRecord
        
        if (isSelected) {
            // 선택 시: 1.0 → RECORD_SELECTED_SCALE 크기로 부드럽게 애니메이션
            val scaleAnimator = ObjectAnimator.ofFloat(1.0f, MarkerConfig.RECORD_SELECTED_SCALE)
            scaleAnimator.duration = 300
            scaleAnimator.interpolator = android.view.animation.OvershootInterpolator(1.8f)
            
            scaleAnimator.addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                marker.icon = createIntermediateRecordMarkerIcon(scale)
            }
            
            scaleAnimator.start()
            
        } else {
            // 해제 시: 현재 크기 → 1.0으로 부드럽게 축소
            val scaleAnimator = ObjectAnimator.ofFloat(MarkerConfig.RECORD_SELECTED_SCALE, 1.0f)
            scaleAnimator.duration = 200
            scaleAnimator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            scaleAnimator.addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                marker.icon = createIntermediateRecordMarkerIcon(scale)
            }
            
            scaleAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    marker.icon = getRecordMarkerIcon(false)
                }
            })
            
            scaleAnimator.start()
        }
    }
    
    private fun createIntermediateRecordMarkerIcon(scale: Float): OverlayImage {
        val drawable = ContextCompat.getDrawable(context, R.drawable.marker_record)
        val size = (MarkerConfig.RECORD_MARKER_SIZE * scale).toInt() // 녹음 마커 기본 크기에 스케일 적용
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }
    
    // 툴팁 뷰 생성 함수들 제거됨 - Compose 툴팁 사용
    
    /**
     * POI 매니저에 현재 마커/클러스터 위치들을 전달
     */
    private fun notifyMarkerPositions() {
        val currentZoom = naverMap.cameraPosition.zoom
        val allPositions = mutableListOf<LatLng>()
        
        // 개별 마커 위치들 추가
        markers.forEach { marker ->
            allPositions.add(marker.position)
        }
        
        // Record 마커 위치들 추가
        recordMarkers.forEach { marker ->
            allPositions.add(marker.position)
        }
        
        // 클러스터 마커 위치들 추가
        clusterMarkers.forEach { marker ->
            allPositions.add(marker.position)
        }
        
        // Record 클러스터 마커 위치들 추가
        recordClusterMarkers.forEach { marker ->
            allPositions.add(marker.position)
        }
        
        Log.d("MapClusterManager", "🎯 POI 매니저에 마커 위치 전달: ${allPositions.size}개 위치, 줌: $currentZoom")
        
        // POI 매니저에 콜백으로 전달
        onMarkerPositionsUpdated?.invoke(allPositions, currentZoom)
    }
}