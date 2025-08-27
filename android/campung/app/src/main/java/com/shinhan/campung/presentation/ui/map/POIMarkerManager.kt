package com.shinhan.campung.presentation.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.shinhan.campung.R
import com.shinhan.campung.data.model.POIData
import android.util.Log

/**
 * POI 마커를 관리하는 클래스
 * 기존 MapClusterManager와 유사한 구조를 가지지만 POI에 특화됨
 */
class POIMarkerManager(
    private val context: Context,
    private val naverMap: NaverMap,
    private val coroutineScope: CoroutineScope
) {
    
    // POI 마커 클릭 콜백
    var onPOIClick: ((POIData) -> Unit)? = null
    
    private val poiMarkers = mutableListOf<Marker>()
    private val poiIconCache = mutableMapOf<String, OverlayImage>()
    private val imageCache = mutableMapOf<String, Bitmap>()
    
    // POI 표시 여부 상태
    private var isPOIVisible = true // 테스트를 위해 기본값을 true로 변경
    
    init {
        Log.d("POIMarkerManager", "POI 마커 매니저 초기화 완료")
    }
    
    /**
     * POI 마커들을 지도에 표시
     */
    fun showPOIMarkers(pois: List<POIData>) {
        if (!isPOIVisible) {
            Log.d("POIMarkerManager", "🏪 POI 비활성화 상태 - 마커 표시 스킵")
            return
        }
        
        Log.d("POIMarkerManager", "🏪 POI 마커 표시 시작 - 요청: ${pois.size}개")
        clearPOIMarkers()
        
        var validCount = 0
        var skippedCount = 0
        
        pois.forEach { poi ->
            // 썸네일 URL이 없으면 마커를 표시하지 않음
            if (poi.thumbnailUrl == null) {
                skippedCount++
                Log.v("POIMarkerManager", "🏪 썸네일 URL 없음으로 스킵: ${poi.name}")
                return@forEach
            }
            
            val marker = Marker().apply {
                position = LatLng(poi.latitude, poi.longitude)
                map = naverMap
                tag = poi
                zIndex = 500 // 일반 마커보다 낮게 설정하여 겹치지 않도록
                
                setOnClickListener {
                    Log.d("POIMarkerManager", "🏪 마커 클릭: ${poi.name}")
                    onPOIClick?.invoke(poi)
                    true
                }
            }
            
            poiMarkers.add(marker)
            validCount++
            
            // 비동기로 이미지 로드
            Log.v("POIMarkerManager", "🏪 이미지 로드 요청: ${poi.name} - ${poi.thumbnailUrl}")
            loadPOIImage(marker, poi.thumbnailUrl!!, poi.category)
        }
        
        Log.d("POIMarkerManager", "🏪 POI 마커 처리 완료 - 유효: ${validCount}개, 스킵: ${skippedCount}개")
    }
    
    /**
     * POI 마커들을 지도에서 제거
     */
    fun clearPOIMarkers() {
        val count = poiMarkers.size
        poiMarkers.forEach { it.map = null }
        poiMarkers.clear()
        Log.d("POIMarkerManager", "🏪 POI 마커 ${count}개 모두 제거됨")
    }
    
    /**
     * POI 표시 상태 토글
     */
    fun togglePOIVisibility(): Boolean {
        isPOIVisible = !isPOIVisible
        Log.d("POIMarkerManager", "🏪 POI 표시 상태 토글: $isPOIVisible")
        if (!isPOIVisible) {
            clearPOIMarkers()
        }
        return isPOIVisible
    }
    
    /**
     * 현재 POI 표시 상태 반환
     */
    fun isPOIVisible(): Boolean = isPOIVisible
    
    
    /**
     * 특정 카테고리의 POI만 필터링하여 표시
     */
    fun filterPOIByCategory(pois: List<POIData>, category: String?) {
        val filteredPOIs = if (category.isNullOrEmpty()) {
            pois
        } else {
            pois.filter { it.category == category }
        }
        
        showPOIMarkers(filteredPOIs)
    }
    
    /**
     * POI 이미지 비동기 로드
     */
    private fun loadPOIImage(marker: Marker, imageUrl: String, category: String) {
        // 캐시에서 먼저 확인
        imageCache[imageUrl]?.let { cachedBitmap ->
            marker.icon = createPOIIconFromBitmap(cachedBitmap)
            return
        }
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d("POIMarkerManager", "🏪 이미지 로드 시작: $imageUrl")
                val bitmap = loadBitmapFromUrl(imageUrl)
                
                if (bitmap != null) {
                    // 캐시에 저장
                    imageCache[imageUrl] = bitmap
                    
                    // 메인 스레드에서 마커 아이콘 업데이트
                    withContext(Dispatchers.Main) {
                        marker.icon = createPOIIconFromBitmap(bitmap)
                        Log.d("POIMarkerManager", "🏪 이미지 로드 완료: $imageUrl")
                    }
                } else {
                    // 이미지 로드 실패 시 마커 제거
                    withContext(Dispatchers.Main) {
                        marker.map = null
                        poiMarkers.remove(marker)
                        Log.w("POIMarkerManager", "🏪 이미지 로드 실패로 마커 제거: $imageUrl")
                    }
                }
            } catch (e: Exception) {
                // 오류 발생 시 마커 제거
                withContext(Dispatchers.Main) {
                    marker.map = null
                    poiMarkers.remove(marker)
                    Log.e("POIMarkerManager", "🏪 이미지 로드 중 오류로 마커 제거: $imageUrl", e)
                }
            }
        }
    }
    
    /**
     * URL에서 Bitmap 로드
     */
    private suspend fun loadBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            withContext(Dispatchers.IO) {
                val url = URL(imageUrl)
                val inputStream = url.openConnection().getInputStream()
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("POIMarkerManager", "Bitmap 로드 실패: $imageUrl", e)
            null
        }
    }
    
    /**
     * Bitmap으로부터 POI 마커 아이콘 생성
     */
    private fun createPOIIconFromBitmap(originalBitmap: Bitmap): OverlayImage {
        val size = 120 // 마커 크기를 80에서 120으로 증가 (약 40dp 추가)
        val borderWidth = 4f
        
        // 원본 비트맵을 정사각형으로 크롭하고 크기 조정
        val croppedBitmap = cropToSquare(originalBitmap)
        val imageSize = size - (borderWidth * 2).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, imageSize, imageSize, true)
        
        // 최종 비트맵 생성
        val finalBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        
        val centerX = size / 2f
        val centerY = size / 2f
        val outerRadius = size / 2f
        val innerRadius = outerRadius - borderWidth
        
        // 흰색 테두리 원 그리기
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, outerRadius, borderPaint)
        
        // 원형으로 클립된 이미지 생성
        val circularBitmap = getCircularBitmap(scaledBitmap)
        
        // 이미지를 중앙에 그리기
        val left = borderWidth
        val top = borderWidth
        canvas.drawBitmap(circularBitmap, left, top, null)
        
        return OverlayImage.fromBitmap(finalBitmap)
    }
    
    /**
     * 비트맵을 원형으로 클립
     */
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        // 원형 마스크 그리기
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // 이미지와 마스크 합성
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return output
    }
    
    /**
     * 비트맵을 정사각형으로 크롭
     */
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }
}