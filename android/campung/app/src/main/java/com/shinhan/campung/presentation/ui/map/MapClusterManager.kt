package com.shinhan.campung.presentation.ui.map

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.ContextCompat
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
    val naverMap: NaverMap
) {

    // 마커 클릭 콜백
    var onMarkerClick: ((MapContent) -> Unit)? = null
    var onClusterClick: ((List<MapContent>) -> Unit)? = null
    var onCenterMarkerChanged: ((MapContent?) -> Unit)? = null

    // 선택된 마커 상태 관리
    var selectedMarker: Marker? = null
        private set
    var selectedContent: MapContent? = null
        private set


    private val markers = mutableListOf<Marker>()
    private val clusterMarkers = mutableListOf<Marker>()

    // 툴팁용 InfoWindow (클릭용)
    private var tooltipInfoWindow: InfoWindow? = null
    // 포커스용 InfoWindow (중앙 근처 마커용)
    private var focusTooltipInfoWindow: InfoWindow? = null

    fun setupClustering() {
        // 툴팁용 InfoWindow 초기화
        tooltipInfoWindow = InfoWindow()
        focusTooltipInfoWindow = InfoWindow()
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

            // 툴팁 InfoWindow를 마커에 직접 붙이기
            showTooltip(targetMarker, content)
        } else {
            Log.e("MapClusterManager", "마커를 찾을 수 없음: ${content.title}")
        }
    }

    // 선택 해제
    fun clearSelection() {
        selectedMarker?.let { marker ->
            // 애니메이션과 함께 아이콘 변경
            animateMarkerSelection(marker, false)
            marker.zIndex = 0
        }
        selectedMarker = null
        selectedContent = null

        // 툴팁 숨김
        hideTooltip()
        Log.d("MapClusterManager", "마커 선택 해제됨")
    }

    // 툴팁 표시
    private fun showTooltip(marker: Marker, content: MapContent) {
        tooltipInfoWindow?.let { infoWindow ->
            Log.d("MapClusterManager", "showTooltip 시작: ${content.title}")
            
            // 간단한 텍스트 어댑터로 테스트
            infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(context) {
                override fun getText(infoWindow: InfoWindow): CharSequence {
                    Log.d("MapClusterManager", "InfoWindow getText 호출됨: ${content.title}")
                    return content.title
                }
            }
            
            // 마커에 InfoWindow 붙이기
            infoWindow.open(marker)
            Log.d("MapClusterManager", "InfoWindow.open() 호출 완료: ${content.title}")
        } ?: run {
            Log.e("MapClusterManager", "tooltipInfoWindow가 null입니다!")
        }
    }
    
    // 툴팁 숨기기
    private fun hideTooltip() {
        tooltipInfoWindow?.close()
        Log.d("MapClusterManager", "툴팁 숨김")
    }
    
    // 포커스 툴팁 표시 (중앙 근처 마커용)
    private fun showFocusTooltip(marker: Marker, content: MapContent) {
        focusTooltipInfoWindow?.let { infoWindow ->
            Log.d("MapClusterManager", "포커스 툴팁 표시: ${content.title}")
            
            // 간단한 텍스트 어댑터로 설정
            infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(context) {
                override fun getText(infoWindow: InfoWindow): CharSequence {
                    return content.title
                }
            }
            
            // 마커에 InfoWindow 붙이기
            infoWindow.open(marker)
        }
    }
    
    // 포커스 툴팁 숨기기
    private fun hideFocusTooltip() {
        focusTooltipInfoWindow?.close()
        Log.d("MapClusterManager", "포커스 툴팁 숨김")
    }

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
        // 선택된 마커가 있으면 하이라이트 변경 안함
        if (selectedMarker != null) return

        // 이전 하이라이트가 새로운 마커와 같으면 중복 처리 방지
        if (highlightedMarker == newMarker) return

        // 이전 하이라이트 제거 (툴팁도 함께)
        highlightedMarker?.let { marker ->
            if (marker != selectedMarker) { // 선택된 마커가 아닐 때만
                animateMarkerFocus(marker, false)
                marker.zIndex = 0
                // 이전 포커스 마커의 툴팁 숨김
                hideFocusTooltip()
            }
        }

        // 새로운 하이라이트 적용
        highlightedMarker = newMarker
        val content = newMarker?.tag as? MapContent

        newMarker?.let { marker ->
            if (marker != selectedMarker) { // 선택된 마커가 아닐 때만
                animateMarkerFocus(marker, true)
                marker.zIndex = 1000
                
                // 새 포커스 마커에 툴팁 표시
                content?.let { showFocusTooltip(marker, it) }
            }
        }

        // 콜백 호출
        onCenterMarkerChanged?.invoke(content)
    }

    private var highlightedMarker: Marker? = null // 접근성을 위해 private으로 변경

    fun updateMarkers(mapContents: List<MapContent>) {
        // 선택된 마커 정보 백업
        val wasSelectedContent = selectedContent

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

        // 이전에 선택된 마커가 있었다면 다시 선택
        wasSelectedContent?.let { prevSelected ->
            val stillExists = mapContents.find { it.contentId == prevSelected.contentId }
            stillExists?.let { content ->
                selectMarker(content)
            }
        }
    }

    private fun showIndividualMarkers(mapContents: List<MapContent>) {
        mapContents.forEach { content ->
            val marker = Marker().apply {
                position = LatLng(content.location.latitude, content.location.longitude)
                icon = createNormalMarkerIcon(content.postType)
                map = naverMap
                tag = content // MapContent 저장

                setOnClickListener {
                    // 이미 선택된 마커를 다시 클릭하면 선택 해제
                    if (selectedContent?.contentId == content.contentId) {
                        clearSelection()
                        onMarkerClick?.invoke(content) // 콜백은 여전히 호출
                    } else {
                        // 새로운 마커 선택
                        selectMarker(content)
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

        Log.d("MapClusterManager", "줌: ${naverMap.cameraPosition.zoom}, 클러스터 거리: ${clusterDistance}m, 생성된 클러스터: ${clusters.size}개")

        clusters.forEach { cluster ->

            if (cluster.size == 1) {
                // 단일 마커
                val content = cluster[0]
                val marker = Marker().apply {
                    position = LatLng(content.location.latitude, content.location.longitude)
                    icon = createNormalMarkerIcon(content.postType)
                    map = naverMap
                    tag = content // MapContent 저장

                    setOnClickListener {
                        // 이미 선택된 마커를 다시 클릭하면 선택 해제
                        if (selectedContent?.contentId == content.contentId) {
                            clearSelection()
                            onMarkerClick?.invoke(content)
                        } else {
                            // 새로운 마커 선택
                            selectMarker(content)
                            onMarkerClick?.invoke(content)
                        }
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
                        // 클러스터 클릭 시 선택 해제
                        clearSelection()

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

        // 선택 상태는 유지 (selectedMarker, selectedContent는 그대로)
        highlightedMarker = null
    }

    fun clearMarkers() {
        clearAllMarkers()
        clearSelection()
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
        val size = (64 * 1.3).toInt() // 1.3배 크기 (선택 시 더 크게)
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }

    // 마커 선택/해제 애니메이션
    private fun animateMarkerSelection(marker: Marker, isSelected: Boolean) {
        if (isSelected) {
            // 선택 시: 작은 크기 → 큰 크기로 애니메이션
            val scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(
                marker,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f, 1.3f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.2f, 1.3f)
            )
            scaleAnimator.duration = 200
            scaleAnimator.start()
            
            // 아이콘 변경
            val content = marker.tag as? MapContent
            marker.icon = createSelectedMarkerIcon(content?.postType)
        } else {
            // 해제 시: 큰 크기 → 작은 크기로 애니메이션
            val scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(
                marker,
                PropertyValuesHolder.ofFloat("scaleX", 1.3f, 1.1f, 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 1.3f, 1.1f, 1.0f)
            )
            scaleAnimator.duration = 200
            scaleAnimator.start()
            
            // 아이콘 변경
            val content = marker.tag as? MapContent
            marker.icon = createNormalMarkerIcon(content?.postType)
        }
    }

    // 마커 포커스 애니메이션 (중앙 근처 마커)
    private fun animateMarkerFocus(marker: Marker, isFocused: Boolean) {
        if (isFocused) {
            // 포커스 시: 부드럽게 1.2배로 확대
            val scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(
                marker,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.2f)
            )
            scaleAnimator.duration = 150
            scaleAnimator.start()
            
            // 아이콘 변경
            val content = marker.tag as? MapContent
            marker.icon = createHighlightedMarkerIcon(content?.postType)
        } else {
            // 포커스 해제 시: 원래 크기로 축소
            val scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(
                marker,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f, 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f, 1.0f)
            )
            scaleAnimator.duration = 150
            scaleAnimator.start()
            
            // 아이콘 변경
            val content = marker.tag as? MapContent
            marker.icon = createNormalMarkerIcon(content?.postType)
        }
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
        val size = (64 * 1.2).toInt() // 1.2배 크기
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
        val size = 64 // drawable의 크기와 맞춤
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888) // 높이를 약간 더 크게
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
    }
}