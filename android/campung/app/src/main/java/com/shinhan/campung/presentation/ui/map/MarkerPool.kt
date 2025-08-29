package com.shinhan.campung.presentation.ui.map

import android.graphics.PointF
import android.util.Log
import com.naver.maps.map.overlay.Marker
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 마커 객체 재사용을 위한 풀 클래스
 * 메모리 할당/해제 비용을 줄이고 GC 압박을 감소시킴
 */
class MarkerPool {
    
    companion object {
        private const val TAG = "MarkerPool"
        private const val INITIAL_POOL_SIZE = 50    // 초기 풀 크기
        private const val MAX_POOL_SIZE = 200       // 최대 풀 크기
        private const val MIN_POOL_SIZE = 20        // 최소 유지 크기
    }
    
    // 사용 가능한 마커들의 풀 (Thread-Safe)
    private val availableMarkers = ConcurrentLinkedQueue<Marker>()
    
    // 현재 사용 중인 마커들 추적
    private val activeMarkers = mutableSetOf<Marker>()
    
    // 풀 통계
    private var totalCreated = 0
    private var totalReused = 0
    private var totalReturned = 0
    
    init {
        // 초기 마커들 미리 생성
        preCreateMarkers(INITIAL_POOL_SIZE)
        Log.d(TAG, "마커 풀 초기화 완료 - 초기 크기: $INITIAL_POOL_SIZE")
    }
    
    /**
     * 마커 풀에서 마커를 가져옴
     * 풀이 비어있으면 새로 생성
     */
    fun acquireMarker(): Marker {
        val marker = availableMarkers.poll()
        
        return if (marker != null) {
            // 풀에서 재사용
            totalReused++
            resetMarker(marker)
            activeMarkers.add(marker)
            Log.v(TAG, "마커 재사용 - 풀 크기: ${availableMarkers.size}, 활성: ${activeMarkers.size}")
            marker
        } else {
            // 새로 생성
            val newMarker = createNewMarker()
            totalCreated++
            activeMarkers.add(newMarker)
            Log.v(TAG, "마커 새로 생성 - 풀 크기: ${availableMarkers.size}, 활성: ${activeMarkers.size}")
            newMarker
        }
    }
    
    /**
     * 마커를 풀로 반환
     */
    fun releaseMarker(marker: Marker) {
        if (!activeMarkers.contains(marker)) {
            Log.w(TAG, "이미 반환된 마커이거나 풀에서 생성되지 않은 마커")
            return
        }
        
        activeMarkers.remove(marker)
        
        // 풀 크기 제한 확인
        if (availableMarkers.size < MAX_POOL_SIZE) {
            // 마커 정리 후 풀에 반환
            cleanupMarker(marker)
            availableMarkers.offer(marker)
            totalReturned++
            Log.v(TAG, "마커 풀 반환 - 풀 크기: ${availableMarkers.size}, 활성: ${activeMarkers.size}")
        } else {
            // 풀이 가득 찬 경우 마커 완전 해제
            marker.map = null
            Log.v(TAG, "풀 가득 참 - 마커 완전 해제")
        }
    }
    
    /**
     * 여러 마커를 일괄 반환
     */
    fun releaseMarkers(markers: Collection<Marker>) {
        markers.forEach { releaseMarker(it) }
        Log.d(TAG, "마커 일괄 반환 완료 - ${markers.size}개")
    }
    
    /**
     * 활성 마커 모두 반환
     */
    fun releaseAllActiveMarkers() {
        val activeList = activeMarkers.toList()
        activeList.forEach { releaseMarker(it) }
        Log.d(TAG, "모든 활성 마커 반환 완료 - ${activeList.size}개")
    }
    
    /**
     * 풀 크기를 동적으로 조정
     */
    fun adjustPoolSize() {
        val currentSize = availableMarkers.size
        val activeSize = activeMarkers.size
        
        // 풀이 너무 큰 경우 축소 (사용률이 낮을 때)
        if (currentSize > MAX_POOL_SIZE * 0.8 && activeSize < currentSize * 0.3) {
            val removeCount = (currentSize - MAX_POOL_SIZE * 0.6).toInt()
            repeat(removeCount) {
                val marker = availableMarkers.poll()
                marker?.let { it.map = null }
            }
            Log.d(TAG, "풀 크기 축소 - ${removeCount}개 제거, 현재 크기: ${availableMarkers.size}")
        }
        
        // 풀이 너무 작은 경우 확장 (사용률이 높을 때)
        else if (currentSize < MIN_POOL_SIZE && activeSize > currentSize * 2) {
            val addCount = MIN_POOL_SIZE - currentSize
            preCreateMarkers(addCount)
            Log.d(TAG, "풀 크기 확장 - ${addCount}개 추가, 현재 크기: ${availableMarkers.size}")
        }
    }
    
    /**
     * 풀 통계 정보 반환
     */
    fun getPoolStats(): PoolStats {
        return PoolStats(
            availableCount = availableMarkers.size,
            activeCount = activeMarkers.size,
            totalCreated = totalCreated,
            totalReused = totalReused,
            totalReturned = totalReturned,
            reuseRatio = if (totalCreated > 0) totalReused.toFloat() / (totalCreated + totalReused) else 0f
        )
    }
    
    /**
     * 풀 정리 (메모리 정리 시 호출)
     */
    fun cleanup() {
        Log.d(TAG, "마커 풀 정리 시작")
        
        // 모든 활성 마커 해제
        activeMarkers.forEach { it.map = null }
        activeMarkers.clear()
        
        // 풀의 모든 마커 해제
        while (availableMarkers.isNotEmpty()) {
            val marker = availableMarkers.poll()
            marker?.let { it.map = null }
        }
        
        Log.d(TAG, "마커 풀 정리 완료 - 생성: $totalCreated, 재사용: $totalReused, 반환: $totalReturned")
    }
    
    // Private Helper Functions
    
    /**
     * 마커들을 미리 생성
     */
    private fun preCreateMarkers(count: Int) {
        repeat(count) {
            val marker = createNewMarker()
            availableMarkers.offer(marker)
        }
        Log.v(TAG, "마커 ${count}개 미리 생성 완료")
    }
    
    /**
     * 새로운 마커 생성
     */
    private fun createNewMarker(): Marker {
        return Marker()
    }
    
    /**
     * 마커를 초기 상태로 리셋 (재사용 전 호출)
     */
    private fun resetMarker(marker: Marker) {
        marker.apply {
            // 기본값으로 리셋
            zIndex = 0
            alpha = 1.0f
            angle = 0.0f
            width = Marker.SIZE_AUTO
            height = Marker.SIZE_AUTO
            anchor = PointF(0.5f, 1.0f)
            captionText = ""
            captionTextSize = 0.0f
            captionColor = 0
            captionHaloColor = 0
            isHideCollidedSymbols = false
            isHideCollidedMarkers = false
            isHideCollidedCaptions = false
            isForceShowIcon = false
            isForceShowCaption = false
            isFlat = false
            // 클릭 리스너와 태그는 사용하는 쪽에서 설정
        }
    }
    
    /**
     * 마커 정리 (풀 반환 전 호출)
     */
    private fun cleanupMarker(marker: Marker) {
        marker.apply {
            // 지도에서 제거
            map = null
            // 리스너 제거
            onClickListener = null
            // 태그 제거  
            tag = null
            // 아이콘을 기본값으로 리셋 (메모리 절약)
            // icon = null // 제거: non-null 타입이므로 null 할당 불가
        }
    }
}

/**
 * 풀 통계 정보 데이터 클래스
 */
data class PoolStats(
    val availableCount: Int,
    val activeCount: Int,
    val totalCreated: Int,
    val totalReused: Int,
    val totalReturned: Int,
    val reuseRatio: Float
) {
    override fun toString(): String {
        return """
            |PoolStats:
            |  Available: $availableCount
            |  Active: $activeCount  
            |  Total Created: $totalCreated
            |  Total Reused: $totalReused
            |  Total Returned: $totalReturned
            |  Reuse Ratio: ${(reuseRatio * 100).toInt()}%
        """.trimMargin()
    }
}