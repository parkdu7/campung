package com.shinhan.campung.presentation.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.InfoWindow
import com.shinhan.campung.data.remote.response.MapContent
import com.shinhan.campung.R
import kotlin.math.*

class MapClusterManager(
    private val context: Context,
    private val naverMap: NaverMap
) {
    
    // 마커 클릭 콜백
    var onMarkerClick: ((MapContent) -> Unit)? = null
    var onClusterClick: ((List<MapContent>) -> Unit)? = null
    var onCenterMarkerChanged: ((MapContent?) -> Unit)? = null
    
    // 현재 하이라이트된 마커
    private var highlightedMarker: Marker? = null
    private var normalMarkerIcon: OverlayImage? = null
    private var highlightedMarkerIcon: OverlayImage? = null
    
    private val markers = mutableListOf<Marker>()
    private val clusterMarkers = mutableListOf<Marker>()
    
    // InfoWindow for tooltip
    private val tooltipInfoWindow = InfoWindow()
    
    fun setupClustering() {
        // 마커 아이콘 초기화
        normalMarkerIcon = createNormalMarkerIcon()
        highlightedMarkerIcon = createHighlightedMarkerIcon()
        
        // 툴팁 InfoWindow 설정
        setupTooltipInfoWindow()
    }
    
    private fun setupTooltipInfoWindow() {
        tooltipInfoWindow.adapter = object : InfoWindow.ViewAdapter() {
            override fun getView(infoWindow: InfoWindow): View {
                return createTooltipView(infoWindow.marker?.tag as? MapContent)
            }
        }
    }
    
    private fun createTooltipView(mapContent: MapContent?): View {
        return TextView(context).apply {
            mapContent?.let { content ->
                text = content.title
                textSize = 12f
                setTextColor(android.graphics.Color.BLACK)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                maxLines = 1
                setPadding(24, 16, 24, 16)
                
                // 말풍선 배경
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE)
                    cornerRadius = 16f
                    setStroke(1, android.graphics.Color.LTGRAY)
                }
                
                // 그림자
                elevation = 8f
            }
        }
    }
    
    fun findCenterMarker(): MapContent? {
        val center = naverMap.cameraPosition.target
        var closestMarker: Marker? = null
        var minDistance = Double.MAX_VALUE
        
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
        
        // 하이라이트 업데이트
        updateHighlightedMarker(closestMarker)
        
        return closestMarker?.tag as? MapContent
    }
    
    private fun updateHighlightedMarker(newMarker: Marker?) {
        // 이전 하이라이트 제거 및 툴팁 숨기기
        highlightedMarker?.let { marker ->
            marker.icon = normalMarkerIcon!!
            marker.zIndex = 0
            tooltipInfoWindow.close()
        }
        
        // 새로운 하이라이트 적용 및 툴팁 표시
        highlightedMarker = newMarker
        newMarker?.let { marker ->
            marker.icon = highlightedMarkerIcon!!
            marker.zIndex = 1000 // 최상위로
            tooltipInfoWindow.open(marker) // 툴팁 표시
        }
        
        // 콜백 호출
        val content = newMarker?.tag as? MapContent
        onCenterMarkerChanged?.invoke(content)
    }
    
    fun updateMarkers(mapContents: List<MapContent>) {
        clearAllMarkers()
        
        val currentZoom = naverMap.cameraPosition.zoom
        Log.d("MapClusterManager", "현재 줌 레벨: $currentZoom, 마커 개수: ${mapContents.size}")
        
        if (currentZoom >= 18) {
            // 높은 줌 레벨: 개별 마커 표시
            Log.d("MapClusterManager", "개별 마커 모드")
            showIndividualMarkers(mapContents)
        } else {
            // 낮은 줌 레벨: 클러스터링 적용
            Log.d("MapClusterManager", "클러스터링 모드")
            showClusteredMarkers(mapContents)
        }
    }
    
    private fun showIndividualMarkers(mapContents: List<MapContent>) {
        mapContents.forEach { content ->
            val marker = Marker().apply {
                position = LatLng(content.location.latitude, content.location.longitude)
                icon = normalMarkerIcon!!
                map = naverMap
                tag = content // MapContent 저장
                
                setOnClickListener {
                    onMarkerClick?.invoke(content)
                    true
                }
            }
            markers.add(marker)
        }
    }
    
    private fun showClusteredMarkers(mapContents: List<MapContent>) {
        val clusterDistance = getClusterDistance()
        val clusters = clusterMarkers(mapContents, clusterDistance)
        
        Log.d("MapClusterManager", "줌: ${naverMap.cameraPosition.zoom}, 클러스터 거리: ${clusterDistance}m, 생성된 클러스터: ${clusters.size}개")
        
        clusters.forEach { cluster ->
            
            if (cluster.size == 1) {
                // 단일 마커
                val content = cluster[0]
                val marker = Marker().apply {
                    position = LatLng(content.location.latitude, content.location.longitude)
                    icon = normalMarkerIcon!!
                    map = naverMap
                    tag = content // MapContent 저장
                    
                    setOnClickListener {
                        onMarkerClick?.invoke(content)
                        true
                    }
                }
                markers.add(marker)
            } else {
                // 클러스터 마커
                val centerLat = cluster.map { it.location.latitude }.average()
                val centerLng = cluster.map { it.location.longitude }.average()
                
                val clusterMarker = Marker().apply {
                    position = LatLng(centerLat, centerLng)
                    captionText = "${cluster.size}개 항목"
                    icon = createClusterIcon(cluster.size)
                    map = naverMap
                    
                    setOnClickListener {
                        // 클러스터 클릭 콜백 먼저 호출
                        onClusterClick?.invoke(cluster)
                        
                        // 기본 동작: 줌인
                        val newZoom = (naverMap.cameraPosition.zoom + 2).coerceAtMost(naverMap.maxZoom)
                        naverMap.moveCamera(
                            CameraUpdate.scrollAndZoomTo(position, newZoom)
                        )
                        true
                    }
                }
                clusterMarkers.add(clusterMarker)
            }
        }
    }
    
    private fun clusterMarkers(mapContents: List<MapContent>, distance: Double): List<List<MapContent>> {
        val clusters = mutableListOf<MutableList<MapContent>>()
        val processed = mutableSetOf<MapContent>()
        
        mapContents.forEach { content ->
            if (content in processed) return@forEach
            
            val cluster = mutableListOf<MapContent>()
            cluster.add(content)
            processed.add(content)
            
            mapContents.forEach { other ->
                if (other != content && other !in processed) {
                    val distance1 = calculateDistance(
                        content.location.latitude, content.location.longitude,
                        other.location.latitude, other.location.longitude
                    )
                    
                    if (distance1 <= distance) {
                        cluster.add(other)
                        processed.add(other)
                    }
                }
            }
            
            clusters.add(cluster)
        }
        
        return clusters
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
            naverMap.cameraPosition.zoom >= 17 -> 20.0   // 20m - 매우 가깝게만
            naverMap.cameraPosition.zoom >= 16 -> 30.0   // 30m - 아주 가깝게
            naverMap.cameraPosition.zoom >= 15 -> 50.0   // 50m - 가깝게만
            naverMap.cameraPosition.zoom >= 14 -> 80.0   // 80m - 조금 가깝게
            naverMap.cameraPosition.zoom >= 13 -> 120.0  // 120m - 적당히
            naverMap.cameraPosition.zoom >= 12 -> 200.0  // 200m
            naverMap.cameraPosition.zoom >= 11 -> 300.0  // 300m
            else -> 500.0 // 500m - 매우 낮은 줌에서만 넓게
        }
    }
    
    private fun clearAllMarkers() {
        markers.forEach { it.map = null }
        markers.clear()
        
        clusterMarkers.forEach { it.map = null }
        clusterMarkers.clear()
    }
    
    fun clearMarkers() {
        clearAllMarkers()
    }
    
    private fun createClusterIcon(count: Int): OverlayImage {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 배경 원 그리기
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FF3F51B5") // Material Blue
            style = Paint.Style.FILL
        }
        
        val radius = size / 2f - 2f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        
        // 테두리 그리기
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        
        // 텍스트 그리기
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = when {
                count < 10 -> 24f
                count < 100 -> 20f
                else -> 16f
            }
        }
        
        val text = if (count > 999) "999+" else count.toString()
        val textY = size / 2f + paint.textSize / 3f
        canvas.drawText(text, size / 2f, textY, paint)
        
        return OverlayImage.fromBitmap(bitmap)
    }
    
    private fun createHighlightedMarkerIcon(): OverlayImage {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 하이라이트 효과 (큰 원)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#33FF5722") // 반투명 오렌지
            style = Paint.Style.FILL
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)
        
        // 메인 마커 (작은 원)
        paint.apply {
            color = Color.parseColor("#FFFF5722") // 진한 오렌지
            style = Paint.Style.FILL
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)
        
        // 테두리
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)
        
        return OverlayImage.fromBitmap(bitmap)
    }
    
    private fun createNormalMarkerIcon(): OverlayImage {
        val size = 36
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 메인 마커 (파란색 원)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FF2196F3") // 파란색
            style = Paint.Style.FILL
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
        
        // 테두리
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
        
        return OverlayImage.fromBitmap(bitmap)
    }
}