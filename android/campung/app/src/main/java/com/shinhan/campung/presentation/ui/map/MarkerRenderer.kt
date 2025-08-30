package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.model.MapRecord
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * 마커 렌더링 우선순위와 점진적 로딩을 관리하는 클래스
 */
class MarkerRenderer(
    private val naverMap: NaverMap,
    private val markerPool: MarkerPool,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "MarkerRenderer"
        private const val BATCH_SIZE = 25              // 한 번에 렌더링할 마커 수
        private const val RENDER_DELAY_MS = 16L        // 16ms = ~60fps
        private const val PRIORITY_RENDER_DISTANCE = 1000.0  // 우선 렌더링 거리 (1km)
    }
    
    // 렌더링 작업 관리
    private var currentRenderJob: Job? = null
    private var isRendering = false
    
    /**
     * 우선순위 기반 마커 렌더링
     */
    suspend fun renderMarkersWithPriority(
        contents: List<MapContent>,
        records: List<MapRecord> = emptyList(),
        iconProvider: MarkerIconProvider,
        onProgress: ((Int, Int) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        // 기존 렌더링 작업 취소
        currentRenderJob?.cancel()
        
        currentRenderJob = scope.launch {
            try {
                isRendering = true
                Log.d(TAG, "🎨 우선순위 마커 렌더링 시작 - Content: ${contents.size}, Record: ${records.size}")
                
                val totalMarkers = contents.size + records.size
                var renderedCount = 0
                
                // 1. Content 마커 우선순위 계산 및 렌더링
                val prioritizedContents = prioritizeContents(contents)
                renderedCount += renderContentsBatched(prioritizedContents, iconProvider) { current, total ->
                    onProgress?.invoke(renderedCount + current, totalMarkers)
                }
                
                // 2. Record 마커 렌더링 (낮은 우선순위)
                if (records.isNotEmpty()) {
                    val prioritizedRecords = prioritizeRecords(records)
                    renderedCount += renderRecordsBatched(prioritizedRecords, iconProvider) { current, total ->
                        onProgress?.invoke(renderedCount + current, totalMarkers)
                    }
                }
                
                Log.d(TAG, "✅ 마커 렌더링 완료 - 총 ${renderedCount}개")
                onComplete?.invoke()
                
            } catch (e: CancellationException) {
                Log.d(TAG, "🚫 마커 렌더링 취소됨")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 마커 렌더링 오류", e)
            } finally {
                isRendering = false
            }
        }
    }
    
    /**
     * Content 마커들을 우선순위에 따라 정렬
     */
    private fun prioritizeContents(contents: List<MapContent>): List<PrioritizedContent> {
        val currentBounds = naverMap.contentBounds
        val center = naverMap.cameraPosition.target
        
        return contents.map { content ->
            val markerPosition = LatLng(content.location.latitude, content.location.longitude)
            val distance = MapBoundsCalculator.calculateDistance(center, markerPosition)
            val isVisible = currentBounds.contains(markerPosition)
            
            val priority = calculateContentPriority(content, distance, isVisible)
            
            PrioritizedContent(content, priority, distance, isVisible)
        }.sortedByDescending { it.priority }
    }
    
    /**
     * Record 마커들을 우선순위에 따라 정렬
     */
    private fun prioritizeRecords(records: List<MapRecord>): List<PrioritizedRecord> {
        val currentBounds = naverMap.contentBounds
        val center = naverMap.cameraPosition.target
        
        return records.map { record ->
            val markerPosition = LatLng(record.location.latitude, record.location.longitude)
            val distance = MapBoundsCalculator.calculateDistance(center, markerPosition)
            val isVisible = currentBounds.contains(markerPosition)
            
            val priority = calculateRecordPriority(record, distance, isVisible)
            
            PrioritizedRecord(record, priority, distance, isVisible)
        }.sortedByDescending { it.priority }
    }
    
    /**
     * Content 마커 우선순위 계산
     */
    private fun calculateContentPriority(
        content: MapContent, 
        distance: Double, 
        isVisible: Boolean
    ): Float {
        var priority = 0f
        
        // 화면 가시성 (최우선)
        if (isVisible) priority += 1000f
        
        // 거리 기반 우선순위 (가까울수록 높음)
        priority += when {
            distance < 100 -> 500f
            distance < 500 -> 300f
            distance < 1000 -> 200f
            distance < 2000 -> 100f
            else -> 50f
        }
        
        // 콘텐츠 타입별 우선순위
        priority += when (content.postType) {
            "HOT" -> 100f
            "NOTICE" -> 80f
            "INFO" -> 60f
            "MARKET" -> 40f
            "FREE" -> 30f
            else -> 20f
        }
        
        // 최신성 우선순위 (더 최근 콘텐츠가 높음)
        // content.createdAt을 이용한 계산은 날짜 파싱이 필요하므로 생략
        
        return priority
    }
    
    /**
     * Record 마커 우선순위 계산
     */
    private fun calculateRecordPriority(
        record: MapRecord,
        distance: Double,
        isVisible: Boolean
    ): Float {
        var priority = 0f
        
        // Record는 Content보다 낮은 기본 우선순위
        if (isVisible) priority += 800f
        
        // 거리 기반 우선순위
        priority += when {
            distance < 50 -> 400f
            distance < 200 -> 250f
            distance < 500 -> 150f
            distance < 1000 -> 100f
            else -> 30f
        }
        
        return priority
    }
    
    /**
     * Content 마커들을 배치로 렌더링
     */
    private suspend fun renderContentsBatched(
        prioritizedContents: List<PrioritizedContent>,
        iconProvider: MarkerIconProvider,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Int {
        var renderedCount = 0
        
        prioritizedContents.chunked(BATCH_SIZE).forEach { batch ->
            // 취소 체크
            scope.coroutineContext.ensureActive()
            
            batch.forEach { prioritizedContent ->
                val marker = markerPool.acquireMarker()
                setupContentMarker(marker, prioritizedContent.content, iconProvider)
                renderedCount++
            }
            
            onProgress?.invoke(renderedCount, prioritizedContents.size)
            
            // 다음 배치 전 프레임 드롭 방지를 위한 딜레이
            if (renderedCount < prioritizedContents.size) {
                delay(RENDER_DELAY_MS)
            }
        }
        
        return renderedCount
    }
    
    /**
     * Record 마커들을 배치로 렌더링
     */
    private suspend fun renderRecordsBatched(
        prioritizedRecords: List<PrioritizedRecord>,
        iconProvider: MarkerIconProvider,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Int {
        var renderedCount = 0
        
        prioritizedRecords.chunked(BATCH_SIZE).forEach { batch ->
            // 취소 체크
            scope.coroutineContext.ensureActive()
            
            batch.forEach { prioritizedRecord ->
                val marker = markerPool.acquireMarker()
                setupRecordMarker(marker, prioritizedRecord.record, iconProvider)
                renderedCount++
            }
            
            onProgress?.invoke(renderedCount, prioritizedRecords.size)
            
            // 다음 배치 전 프레임 드롭 방지를 위한 딜레이
            if (renderedCount < prioritizedRecords.size) {
                delay(RENDER_DELAY_MS)
            }
        }
        
        return renderedCount
    }
    
    /**
     * Content 마커 설정
     */
    private fun setupContentMarker(
        marker: Marker, 
        content: MapContent, 
        iconProvider: MarkerIconProvider
    ) {
        marker.apply {
            position = LatLng(content.location.latitude, content.location.longitude)
            icon = iconProvider.getNormalContentIcon(content.postType)
            map = naverMap
            tag = content
            zIndex = 50
        }
    }
    
    /**
     * Record 마커 설정
     */
    private fun setupRecordMarker(
        marker: Marker,
        record: MapRecord,
        iconProvider: MarkerIconProvider
    ) {
        marker.apply {
            position = LatLng(record.location.latitude, record.location.longitude)
            icon = iconProvider.getNormalRecordIcon()
            map = naverMap
            tag = record
            zIndex = 50
        }
    }
    
    /**
     * 렌더링 취소
     */
    fun cancelRendering() {
        currentRenderJob?.cancel()
        Log.d(TAG, "마커 렌더링 취소됨")
    }
    
    /**
     * 현재 렌더링 상태
     */
    fun isCurrentlyRendering(): Boolean = isRendering
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        cancelRendering()
        Log.d(TAG, "MarkerRenderer 정리 완료")
    }
}

/**
 * 마커 아이콘 제공 인터페이스
 */
interface MarkerIconProvider {
    fun getNormalContentIcon(postType: String?): com.naver.maps.map.overlay.OverlayImage
    fun getSelectedContentIcon(postType: String?): com.naver.maps.map.overlay.OverlayImage
    fun getHighlightedContentIcon(postType: String?): com.naver.maps.map.overlay.OverlayImage
    fun getNormalRecordIcon(): com.naver.maps.map.overlay.OverlayImage
    fun getSelectedRecordIcon(): com.naver.maps.map.overlay.OverlayImage
    fun getClusterIcon(count: Int, isSelected: Boolean): com.naver.maps.map.overlay.OverlayImage
}

/**
 * 우선순위가 계산된 Content 데이터
 */
private data class PrioritizedContent(
    val content: MapContent,
    val priority: Float,
    val distance: Double,
    val isVisible: Boolean
)

/**
 * 우선순위가 계산된 Record 데이터
 */
private data class PrioritizedRecord(
    val record: MapRecord,
    val priority: Float,
    val distance: Double,
    val isVisible: Boolean
)