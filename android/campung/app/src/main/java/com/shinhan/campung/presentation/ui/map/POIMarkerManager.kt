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
import kotlin.math.*

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
    
    // 마커/클러스터 충돌 감지를 위한 위치 저장  
    private var existingMarkerPositions = mutableListOf<LatLng>()
    private var currentZoomLevel = 16.0
    
    // 현재 표시 중인 POI 데이터 저장 (재배치용)
    private var currentPOIData = mutableListOf<POIData>()
    
    // 화면 기반 충돌 감지 설정
    private val COLLISION_DETECTION_MIN_ZOOM = 19.0  // 줌 19 이상에서만 충돌 감지
    private val MARKER_COLLISION_RADIUS_PX = 60      // 마커 충돌 반경 (픽셀)
    
    init {
        Log.d("POIMarkerManager", "POI 마커 매니저 초기화 완료")
    }
    
    /**
     * 기존 마커/클러스터 위치 업데이트 + 실시간 POI 재배치
     */
    fun updateExistingMarkerPositions(positions: List<LatLng>, zoomLevel: Double) {
        val wasCollisionActive = currentZoomLevel >= COLLISION_DETECTION_MIN_ZOOM
        
        existingMarkerPositions.clear()
        existingMarkerPositions.addAll(positions)
        currentZoomLevel = zoomLevel
        
        val isCollisionActive = currentZoomLevel >= COLLISION_DETECTION_MIN_ZOOM
        
        Log.d("POIMarkerManager", "🏪 기존 마커 위치 업데이트: ${positions.size}개, 줌: $zoomLevel")
        Log.d("POIMarkerManager", "🏪 충돌 감지 활성화: $isCollisionActive (줌 $COLLISION_DETECTION_MIN_ZOOM 이상)")
        
        // 줌 레벨이 충돌 감지 임계값을 넘나들거나, 이미 활성화 상태에서 마커 위치가 변경된 경우 POI 재배치
        if (isCollisionActive && currentPOIData.isNotEmpty()) {
            Log.w("POIMarkerManager", "🏪 🔄 실시간 POI 재배치 시작 (줌: $zoomLevel)")
            redistributePOIMarkersRealtime()
        } else if (!isCollisionActive && wasCollisionActive && currentPOIData.isNotEmpty()) {
            Log.w("POIMarkerManager", "🏪 🔄 줌 아웃으로 POI 원위치 복원")
            restorePOIToOriginalPositions()
        }
        
        // 디버깅을 위해 첫 3개 위치 출력
        positions.take(3).forEachIndexed { index, position ->
            Log.v("POIMarkerManager", "🏪 [DEBUG] 기존 마커[$index]: (${position.latitude}, ${position.longitude})")
        }
    }
    
    /**
     * POI 마커들을 지도에 표시 (화면 좌표 기반 충돌 감지)
     */
    fun showPOIMarkers(pois: List<POIData>) {
        Log.w("POIMarkerManager", "🏪 === showPOIMarkers 호출됨 ===")
        Log.w("POIMarkerManager", "🏪 POI 가시성: $isPOIVisible")
        Log.w("POIMarkerManager", "🏪 요청된 POI: ${pois.size}개")
        Log.w("POIMarkerManager", "🏪 현재 줌 레벨: $currentZoomLevel")
        
        if (!isPOIVisible) {
            Log.e("POIMarkerManager", "🏪 POI 비활성화 상태 - 마커 표시 스킵")
            return
        }
        
        clearPOIMarkers()
        
        // 현재 POI 데이터 저장 (실시간 재배치용)
        currentPOIData.clear()
        currentPOIData.addAll(pois)
        
        var validCount = 0
        var skippedCount = 0
        var offsetCount = 0
        
        val isCollisionDetectionActive = currentZoomLevel >= COLLISION_DETECTION_MIN_ZOOM
        Log.w("POIMarkerManager", "🏪 충돌 감지 활성화: $isCollisionDetectionActive (줌 $COLLISION_DETECTION_MIN_ZOOM 이상)")
        
        pois.forEach { poi ->
            // 썸네일 URL이 없으면 마커를 표시하지 않음
            if (poi.thumbnailUrl == null) {
                skippedCount++
                Log.v("POIMarkerManager", "🏪 썸네일 URL 없음으로 스킵: ${poi.name}")
                return@forEach
            }
            
            val originalPosition = LatLng(poi.latitude, poi.longitude)
            Log.d("POIMarkerManager", "🏪 [DEBUG] POI 처리 시작: ${poi.name} - 위치: (${poi.latitude}, ${poi.longitude})")
            
            // 화면 좌표 기반 충돌 감지 (줌 19 이상에서만)
            val finalPosition = if (isCollisionDetectionActive) {
                calculateOptimalPositionScreenBased(originalPosition, poi.name)
            } else {
                originalPosition // 줌이 낮으면 원위치
            }
            
            if (finalPosition != originalPosition) {
                offsetCount++
                Log.e("POIMarkerManager", "🏪 ✨ POI 오프셋 적용됨: ${poi.name}")
                Log.e("POIMarkerManager", "🏪 ✨ 원래: (${poi.latitude}, ${poi.longitude})")
                Log.e("POIMarkerManager", "🏪 ✨ 최종: (${finalPosition.latitude}, ${finalPosition.longitude})")
            } else {
                Log.d("POIMarkerManager", "🏪 POI 오프셋 없음: ${poi.name}")
            }
            
            val marker = Marker().apply {
                position = finalPosition
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
        
        Log.d("POIMarkerManager", "🏪 POI 마커 처리 완료 - 유효: ${validCount}개, 스킵: ${skippedCount}개, 오프셋: ${offsetCount}개")
    }
    
    /**
     * POI 마커들을 지도에서 제거
     */
    fun clearPOIMarkers() {
        val count = poiMarkers.size
        poiMarkers.forEach { marker ->
            marker.map = null
            marker.onClickListener = null // 클릭 리스너 제거
            marker.tag = null // 태그 제거
        }
        poiMarkers.clear()
        Log.d("POIMarkerManager", "🏪 POI 마커 ${count}개 모두 제거됨 (리스너 및 태그 정리 완료)")
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
        val size = MarkerConfig.POI_MARKER_SIZE // 중앙 관리되는 마커 크기
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
    
    /**
     * POI 마커의 최적 위치 계산 (동적 마커 위치 리스트 사용)
     */
    private fun calculateOptimalPositionDynamic(
        originalPosition: LatLng, 
        poiName: String, 
        currentMarkerPositions: List<LatLng>
    ): LatLng {
        Log.w("POIMarkerManager", "🏪 === 동적 위치 계산 시작 ===")
        Log.w("POIMarkerManager", "🏪 POI: $poiName")
        Log.w("POIMarkerManager", "🏪 원본 위치: (${originalPosition.latitude}, ${originalPosition.longitude})")
        Log.w("POIMarkerManager", "🏪 현재 마커 개수: ${currentMarkerPositions.size}개 (기존 ${existingMarkerPositions.size}개 + 배치된 POI ${currentMarkerPositions.size - existingMarkerPositions.size}개)")
        Log.w("POIMarkerManager", "🏪 현재 줌 레벨: $currentZoomLevel")
        
        // 줌 레벨이 너무 낮으면 충돌 감지 안함 (14 이상에서만)
        if (currentZoomLevel < 14.0) {
            Log.w("POIMarkerManager", "🏪 줌 레벨 너무 낮음 ($currentZoomLevel) - 충돌 감지 안함")
            return originalPosition
        }
        
        // 기존 마커들이 없으면 충돌 검사 불필요
        if (currentMarkerPositions.isEmpty()) {
            Log.w("POIMarkerManager", "🏪 현재 마커 없음 - 원본 위치 사용")
            return originalPosition
        }
        
        // 줌 레벨별 충돌 감지 반경 (미터 단위)
        val collisionRadiusMeters = when {
            currentZoomLevel >= 18 -> 20.0  // 고줌: 20m
            currentZoomLevel >= 16 -> 35.0  // 중줌: 35m 
            currentZoomLevel >= 14 -> 50.0  // 저줌: 50m
            else -> 0.0                     // 매우 저줌: 충돌 감지 안함
        }
        
        Log.w("POIMarkerManager", "🏪 충돌 감지 반경: ${collisionRadiusMeters}m")
        
        // 충돌 체크 (동적 리스트 사용)
        val hasCollision = hasCollisionWithPositions(originalPosition, collisionRadiusMeters, currentMarkerPositions)
        Log.e("POIMarkerManager", "🏪 🚨 충돌 감지 결과: $hasCollision")
        
        if (!hasCollision) {
            Log.w("POIMarkerManager", "🏪 충돌 없음 - 원래 위치 사용")
            return originalPosition
        }
        
        Log.e("POIMarkerManager", "🏪 🚨 충돌 감지됨! 오프셋 계산 시작")
        
        // 오프셋 거리 (미터 단위)
        val offsetDistanceMeters = when {
            currentZoomLevel >= 18 -> 40.0  // 고줌: 40m
            currentZoomLevel >= 16 -> 60.0  // 중줌: 60m
            currentZoomLevel >= 14 -> 80.0  // 저줌: 80m
            else -> 100.0
        }
        
        Log.e("POIMarkerManager", "🏪 오프셋 거리: ${offsetDistanceMeters}m")
        
        // 8방향으로 오프셋 위치 계산
        val offsetPositions = calculateOffsetPositions(originalPosition, offsetDistanceMeters)
        
        // 충돌이 없는 첫 번째 위치 찾기
        offsetPositions.forEachIndexed { index, position ->
            if (!hasCollisionWithPositions(position, collisionRadiusMeters, currentMarkerPositions)) {
                Log.e("POIMarkerManager", "🏪 ✅ 오프셋 위치 찾음: 방향 $index")
                Log.e("POIMarkerManager", "🏪 ✅ 최종 위치: (${position.latitude}, ${position.longitude})")
                return position
            }
        }
        
        // 모든 위치에 충돌이 있으면 원래 위치 사용 (겹쳐도 표시)
        Log.e("POIMarkerManager", "🏪 ❌ 모든 후보 위치에 충돌 - 원래 위치 강제 사용")
        return originalPosition
    }

    /**
     * POI 마커의 최적 위치 계산 (기존 방식 - 호환성 유지)
     */
    private fun calculateOptimalPosition(originalPosition: LatLng, poiName: String): LatLng {
        Log.w("POIMarkerManager", "🏪 === 위치 계산 시작 ===")
        Log.w("POIMarkerManager", "🏪 POI: $poiName")
        Log.w("POIMarkerManager", "🏪 원본 위치: (${originalPosition.latitude}, ${originalPosition.longitude})")
        Log.w("POIMarkerManager", "🏪 기존 마커 개수: ${existingMarkerPositions.size}개")
        Log.w("POIMarkerManager", "🏪 현재 줌 레벨: $currentZoomLevel")
        
        // 줌 레벨이 너무 낮으면 충돌 감지 안함 (14 이상에서만)
        if (currentZoomLevel < 14.0) {
            Log.w("POIMarkerManager", "🏪 줌 레벨 너무 낮음 ($currentZoomLevel) - 충돌 감지 안함")
            return originalPosition
        }
        
        // 기존 마커들이 없으면 충돌 검사 불필요
        if (existingMarkerPositions.isEmpty()) {
            Log.w("POIMarkerManager", "🏪 기존 마커 없음 - 원본 위치 사용")
            return originalPosition
        }
        
        // 줌 레벨별 충돌 감지 반경 (미터 단위)
        val collisionRadiusMeters = when {
            currentZoomLevel >= 18 -> 20.0  // 고줌: 20m
            currentZoomLevel >= 16 -> 35.0  // 중줌: 35m 
            currentZoomLevel >= 14 -> 50.0  // 저줌: 50m
            else -> 0.0                     // 매우 저줌: 충돌 감지 안함
        }
        
        Log.w("POIMarkerManager", "🏪 충돌 감지 반경: ${collisionRadiusMeters}m")
        
        // 충돌 체크
        val hasCollision = hasCollisionSimple(originalPosition, collisionRadiusMeters)
        Log.e("POIMarkerManager", "🏪 🚨 충돌 감지 결과: $hasCollision")
        
        if (!hasCollision) {
            Log.w("POIMarkerManager", "🏪 충돌 없음 - 원래 위치 사용")
            return originalPosition
        }
        
        Log.e("POIMarkerManager", "🏪 🚨 충돌 감지됨! 오프셋 계산 시작")
        
        // 오프셋 거리 (미터 단위)
        val offsetDistanceMeters = when {
            currentZoomLevel >= 18 -> 40.0  // 고줌: 40m
            currentZoomLevel >= 16 -> 60.0  // 중줌: 60m
            currentZoomLevel >= 14 -> 80.0  // 저줌: 80m
            else -> 100.0
        }
        
        Log.e("POIMarkerManager", "🏪 오프셋 거리: ${offsetDistanceMeters}m")
        
        // 8방향으로 오프셋 위치 계산
        val offsetPositions = calculateOffsetPositions(originalPosition, offsetDistanceMeters)
        
        // 충돌이 없는 첫 번째 위치 찾기
        offsetPositions.forEachIndexed { index, position ->
            if (!hasCollisionSimple(position, collisionRadiusMeters)) {
                Log.e("POIMarkerManager", "🏪 ✅ 오프셋 위치 찾음: 방향 $index")
                Log.e("POIMarkerManager", "🏪 ✅ 최종 위치: (${position.latitude}, ${position.longitude})")
                return position
            }
        }
        
        // 모든 위치에 충돌이 있으면 원래 위치 사용 (겹쳐도 표시)
        Log.e("POIMarkerManager", "🏪 ❌ 모든 후보 위치에 충돌 - 원래 위치 강제 사용")
        return originalPosition
    }
    
    /**
     * 동적 위치 리스트를 사용한 충돌 감지 (POI 간 충돌 방지용)
     */
    private fun hasCollisionWithPositions(
        position: LatLng, 
        collisionRadiusMeters: Double,
        markerPositions: List<LatLng>
    ): Boolean {
        var minDistance = Double.MAX_VALUE
        var collisionCount = 0
        
        Log.w("POIMarkerManager", "🏪 === 동적 충돌 감지 시작 ===")
        Log.w("POIMarkerManager", "🏪 체크할 위치: (${position.latitude}, ${position.longitude})")
        Log.w("POIMarkerManager", "🏪 충돌 기준 반경: ${collisionRadiusMeters}m")
        Log.w("POIMarkerManager", "🏪 비교할 마커 개수: ${markerPositions.size}개")
        
        markerPositions.forEachIndexed { index, existingPosition ->
            val distanceMeters = calculateDistanceMeters(position, existingPosition)
            minDistance = minOf(minDistance, distanceMeters)
            
            Log.w("POIMarkerManager", "🏪 마커[$index]: 거리 ${distanceMeters.toInt()}m")
            
            if (distanceMeters <= collisionRadiusMeters) {
                collisionCount++
                Log.e("POIMarkerManager", "🏪 🚨 충돌! 마커[$index]: ${distanceMeters.toInt()}m ≤ ${collisionRadiusMeters.toInt()}m")
            }
        }
        
        Log.w("POIMarkerManager", "🏪 === 동적 충돌 체크 완료 ===")
        Log.w("POIMarkerManager", "🏪 최소거리: ${minDistance.toInt()}m")
        Log.w("POIMarkerManager", "🏪 충돌 개수: ${collisionCount}개")
        Log.w("POIMarkerManager", "🏪 충돌 여부: ${collisionCount > 0}")
        
        return collisionCount > 0
    }

    /**
     * 간단한 충돌 감지 (미터 단위로 직접 계산)
     */
    private fun hasCollisionSimple(position: LatLng, collisionRadiusMeters: Double): Boolean {
        var minDistance = Double.MAX_VALUE
        var collisionCount = 0
        
        Log.w("POIMarkerManager", "🏪 === 충돌 감지 시작 ===")
        Log.w("POIMarkerManager", "🏪 체크할 위치: (${position.latitude}, ${position.longitude})")
        Log.w("POIMarkerManager", "🏪 충돌 기준 반경: ${collisionRadiusMeters}m")
        Log.w("POIMarkerManager", "🏪 기존 마커 개수: ${existingMarkerPositions.size}개")
        
        existingMarkerPositions.forEachIndexed { index, existingPosition ->
            val distanceMeters = calculateDistanceMeters(position, existingPosition)
            minDistance = minOf(minDistance, distanceMeters)
            
            Log.w("POIMarkerManager", "🏪 마커[$index]: 거리 ${distanceMeters.toInt()}m")
            
            if (distanceMeters <= collisionRadiusMeters) {
                collisionCount++
                Log.e("POIMarkerManager", "🏪 🚨 충돌! 마커[$index]: ${distanceMeters.toInt()}m ≤ ${collisionRadiusMeters.toInt()}m")
            }
        }
        
        Log.w("POIMarkerManager", "🏪 === 충돌 체크 완료 ===")
        Log.w("POIMarkerManager", "🏪 최소거리: ${minDistance.toInt()}m")
        Log.w("POIMarkerManager", "🏪 충돌 개수: ${collisionCount}개")
        Log.w("POIMarkerManager", "🏪 충돌 여부: ${collisionCount > 0}")
        
        return collisionCount > 0
    }
    
    /**
     * 8방향으로 오프셋된 위치들 계산 (미터 단위)
     */
    private fun calculateOffsetPositions(center: LatLng, offsetDistanceMeters: Double): List<LatLng> {
        val positions = mutableListOf<LatLng>()
        val angleStep = 2 * PI / 8 // 45도씩 8방향
        
        repeat(8) { i ->
            val angle = i * angleStep
            
            // 미터를 위도/경도 오프셋으로 변환
            val offsetLat = offsetDistanceMeters * cos(angle) / 111320.0 // 위도 1도 ≈ 111320m
            val offsetLng = offsetDistanceMeters * sin(angle) / (111320.0 * cos(Math.toRadians(center.latitude)))
            
            val newPosition = LatLng(
                center.latitude + offsetLat,
                center.longitude + offsetLng
            )
            positions.add(newPosition)
        }
        
        return positions
    }
    
    /**
     * 두 지점 간의 실제 거리 계산 (미터 단위) - 하버사인 공식
     */
    private fun calculateDistanceMeters(pos1: LatLng, pos2: LatLng): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        val dLat = Math.toRadians(pos2.latitude - pos1.latitude)
        val dLng = Math.toRadians(pos2.longitude - pos1.longitude)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(pos1.latitude)) * cos(Math.toRadians(pos2.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    // ===== 🆕 화면 좌표 기반 충돌 감지 시스템 =====
    
    /**
     * 화면 좌표 기반 POI 최적 위치 계산 (줌 19 이상)
     */
    private fun calculateOptimalPositionScreenBased(originalPosition: LatLng, poiName: String): LatLng {
        Log.e("POIMarkerManager", "🏪 🖼️ === 화면 기반 위치 계산 시작 ===")
        Log.e("POIMarkerManager", "🏪 🖼️ POI: $poiName")
        Log.e("POIMarkerManager", "🏪 🖼️ 줌: $currentZoomLevel (임계값: $COLLISION_DETECTION_MIN_ZOOM)")
        Log.e("POIMarkerManager", "🏪 🖼️ 기존 마커: ${existingMarkerPositions.size}개")
        
        if (existingMarkerPositions.isEmpty()) {
            Log.e("POIMarkerManager", "🏪 🖼️ 기존 마커 없음 - 원위치 사용")
            return originalPosition
        }
        
        // 1. POI 원래 위치를 화면 좌표로 변환
        val poiScreenPoint = naverMap.projection.toScreenLocation(originalPosition)
        Log.e("POIMarkerManager", "🏪 🖼️ POI 화면 좌표: (${poiScreenPoint.x}, ${poiScreenPoint.y})")
        
        // 2. 기존 마커들의 화면 좌표 계산
        val existingScreenPoints = existingMarkerPositions.map { position ->
            naverMap.projection.toScreenLocation(position)
        }
        
        // 3. 화면상 충돌 감지
        var hasCollision = false
        existingScreenPoints.forEachIndexed { index, screenPoint ->
            val pixelDistance = sqrt(
                (poiScreenPoint.x - screenPoint.x).toDouble().pow(2) + 
                (poiScreenPoint.y - screenPoint.y).toDouble().pow(2)
            )
            Log.e("POIMarkerManager", "🏪 🖼️ 마커[$index] 픽셀 거리: ${pixelDistance.toInt()}px (충돌 반경: ${MARKER_COLLISION_RADIUS_PX}px)")
            
            if (pixelDistance <= MARKER_COLLISION_RADIUS_PX) {
                hasCollision = true
                Log.e("POIMarkerManager", "🏪 🚨 화면상 충돌 감지! 마커[$index]: ${pixelDistance.toInt()}px ≤ ${MARKER_COLLISION_RADIUS_PX}px")
            }
        }
        
        if (!hasCollision) {
            Log.e("POIMarkerManager", "🏪 🖼️ 충돌 없음 - 원위치 사용")
            return originalPosition
        }
        
        // 4. 충돌 시 오프셋 적용
        Log.e("POIMarkerManager", "🏪 🚨 충돌 감지됨! 화면 기반 오프셋 계산")
        
        val offsetDistance = MARKER_COLLISION_RADIUS_PX + 20 // 여유 공간
        val angles = listOf(0, 45, 90, 135, 180, 225, 270, 315) // 8방향
        
        for (angle in angles) {
            val radians = Math.toRadians(angle.toDouble())
            val offsetX = cos(radians) * offsetDistance
            val offsetY = sin(radians) * offsetDistance
            
            val offsetScreenPoint = android.graphics.PointF(
                (poiScreenPoint.x + offsetX).toFloat(),
                (poiScreenPoint.y + offsetY).toFloat()
            )
            
            // 오프셋된 화면 좌표를 지리 좌표로 변환
            val offsetPosition = naverMap.projection.fromScreenLocation(offsetScreenPoint)
            
            // 오프셋 위치에서도 충돌 체크
            var offsetHasCollision = false
            existingScreenPoints.forEach { screenPoint ->
                val pixelDistance = sqrt(
                    (offsetScreenPoint.x - screenPoint.x).toDouble().pow(2) + 
                    (offsetScreenPoint.y - screenPoint.y).toDouble().pow(2)
                )
                if (pixelDistance <= MARKER_COLLISION_RADIUS_PX) {
                    offsetHasCollision = true
                }
            }
            
            if (!offsetHasCollision) {
                Log.e("POIMarkerManager", "🏪 ✅ 화면 기반 오프셋 위치 찾음: ${angle}도")
                Log.e("POIMarkerManager", "🏪 ✅ 화면 좌표: (${offsetScreenPoint.x}, ${offsetScreenPoint.y})")
                Log.e("POIMarkerManager", "🏪 ✅ 지리 좌표: (${offsetPosition.latitude}, ${offsetPosition.longitude})")
                return offsetPosition
            }
        }
        
        Log.e("POIMarkerManager", "🏪 ❌ 모든 오프셋 위치에 충돌 - 원위치 강제 사용")
        return originalPosition
    }
    
    /**
     * 실시간 POI 재배치 (줌/이동 시 호출)
     */
    private fun redistributePOIMarkersRealtime() {
        if (currentPOIData.isEmpty()) return
        
        Log.e("POIMarkerManager", "🏪 🔄 === 실시간 POI 재배치 ===")
        Log.e("POIMarkerManager", "🏪 🔄 대상 POI: ${currentPOIData.size}개")
        
        var repositionedCount = 0
        
        poiMarkers.forEachIndexed { index, marker ->
            val poi = marker.tag as? POIData ?: return@forEachIndexed
            val originalPosition = LatLng(poi.latitude, poi.longitude)
            val newPosition = calculateOptimalPositionScreenBased(originalPosition, poi.name)
            
            if (newPosition != originalPosition) {
                marker.position = newPosition
                repositionedCount++
                Log.e("POIMarkerManager", "🏪 🔄 POI 재배치: ${poi.name}")
            }
        }
        
        Log.e("POIMarkerManager", "🏪 🔄 재배치 완료: ${repositionedCount}/${poiMarkers.size}개")
    }
    
    /**
     * POI를 원래 위치로 복원 (줌 아웃 시)
     */
    private fun restorePOIToOriginalPositions() {
        if (currentPOIData.isEmpty()) return
        
        Log.e("POIMarkerManager", "🏪 🔄 === POI 원위치 복원 ===")
        
        var restoredCount = 0
        
        poiMarkers.forEach { marker ->
            val poi = marker.tag as? POIData ?: return@forEach
            val originalPosition = LatLng(poi.latitude, poi.longitude)
            
            if (marker.position != originalPosition) {
                marker.position = originalPosition
                restoredCount++
                Log.e("POIMarkerManager", "🏪 🔄 POI 원위치 복원: ${poi.name}")
            }
        }
        
        Log.e("POIMarkerManager", "🏪 🔄 원위치 복원 완료: ${restoredCount}/${poiMarkers.size}개")
    }
}