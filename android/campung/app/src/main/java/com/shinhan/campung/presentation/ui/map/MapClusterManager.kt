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
import com.shinhan.campung.R
import kotlin.math.*

class MapClusterManager(
    private val context: Context,
    val naverMap: NaverMap,
    private val mapContainer: ViewGroup? = null // 지도를 포함하는 컨테이너 뷰
) {

    // 마커 클릭 콜백
    var onMarkerClick: ((MapContent) -> Unit)? = null
    var onClusterClick: ((List<MapContent>) -> Unit)? = null
    var onCenterMarkerChanged: ((MapContent?) -> Unit)? = null
    
    // 툴팁 콜백 (InfoWindow 대신 사용)
    var onShowTooltip: ((MapContent, com.shinhan.campung.presentation.ui.components.TooltipType) -> Unit)? = null
    var onHideTooltip: (() -> Unit)? = null

    // 선택된 마커 상태 관리
    var selectedMarker: Marker? = null
        private set
    var selectedContent: MapContent? = null
        private set
        
    // 클러스터 클릭으로 인한 카메라 이동 플래그
    var isClusterMoving = false
        private set
        
    // 선택된 클러스터 마커 상태 관리
    var selectedClusterMarker: Marker? = null
        private set


    private val markers = mutableListOf<Marker>()
    private val clusterMarkers = mutableListOf<Marker>()
    private var quadTree: QuadTree? = null
    
    // 아이콘 캐싱 시스템
    private val normalIconCache = mutableMapOf<String, OverlayImage>()
    private val selectedIconCache = mutableMapOf<String, OverlayImage>()
    private val highlightedIconCache = mutableMapOf<String, OverlayImage>()
    private val clusterIconCache = mutableMapOf<String, OverlayImage>()

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
        
        // 클러스터 아이콘도 자주 사용되는 크기들 미리 생성
        val commonClusterSizes = listOf(2, 3, 4, 5, 10, 20, 50, 100)
        commonClusterSizes.forEach { size ->
            clusterIconCache["normal_$size"] = createClusterIconInternal(size, false)
            clusterIconCache["selected_$size"] = createClusterIconInternal(size, true)
        }
        
        Log.d("MapClusterManager", "아이콘 캐시 초기화 완료 - Normal: ${normalIconCache.size}, Selected: ${selectedIconCache.size}, Highlighted: ${highlightedIconCache.size}, Cluster: ${clusterIconCache.size}")
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

    // 선택 해제
    fun clearSelection() {
        selectedMarker?.let { marker ->
            // 애니메이션과 함께 아이콘 변경
            animateMarkerSelection(marker, false)
            marker.zIndex = 0
        }
        selectedMarker = null
        selectedContent = null

        // 선택된 클러스터도 해제
        selectedClusterMarker?.let { clusterMarker ->
            val count = clusterMarker.captionText.replace("개 항목", "").toIntOrNull() ?: 1
            clusterMarker.icon = getClusterIcon(count, false)
            clusterMarker.zIndex = 0
        }
        selectedClusterMarker = null

        // 하이라이트된 마커도 함께 해제
        highlightedMarker?.let { marker ->
            animateMarkerFocus(marker, false)
            marker.zIndex = 0
        }
        highlightedMarker = null

        // Compose 툴팁 숨김
        onHideTooltip?.invoke()
        Log.d("MapClusterManager", "마커 및 클러스터 선택 해제됨")
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
        // 선택된 마커가 있으면 하이라이트 변경 안함
        if (selectedMarker != null) return

        // 이전 하이라이트가 새로운 마커와 같으면 중복 처리 방지
        if (highlightedMarker == newMarker) return

        // 이전 하이라이트 제거 (툴팁도 함께)
        highlightedMarker?.let { marker ->
            if (marker != selectedMarker) { // 선택된 마커가 아닐 때만
                animateMarkerFocus(marker, false)
                marker.zIndex = 0
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
                
                // 새 포커스 마커에 Compose 툴팁 표시 (딜레이 적용)
                content?.let { 
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        onShowTooltip?.invoke(it, com.shinhan.campung.presentation.ui.components.TooltipType.FOCUS)
                    }, 100) // 포커스 애니메이션 후에 툴팁 표시
                }
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

        // 모든 줌 레벨에서 클러스터링 적용 (최대 줌까지)
        Log.d("MapClusterManager", "클러스터링 모드 (줌 레벨: $currentZoom)")
        showClusteredMarkers(mapContents)

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

        Log.d("MapClusterManager", "줌: ${naverMap.cameraPosition.zoom}, 클러스터 거리: ${clusterDistance}m, 생성된 클러스터: ${clusters.size}개")

        clusters.forEach { cluster ->

            if (cluster.size == 1) {
                // 단일 마커
                val content = cluster[0]
                val marker = Marker().apply {
                    position = LatLng(content.location.latitude, content.location.longitude)
                    icon = getNormalMarkerIcon(content.postType)
                    map = naverMap
                    tag = content // MapContent 저장

                    setOnClickListener {
                        // 이미 선택된 마커를 다시 클릭하면 선택 해제
                        if (selectedContent?.contentId == content.contentId) {
                            clearSelection()
                            onMarkerClick?.invoke(content)
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
            } else {
                // 클러스터 마커
                val centerLat = cluster.map { it.location.latitude }.average()
                val centerLng = cluster.map { it.location.longitude }.average()

                val clusterMarker = Marker().apply {
                    position = LatLng(centerLat, centerLng)
                    captionText = "${cluster.size}개 항목"
                    icon = getClusterIcon(cluster.size, false)
                    map = naverMap

                    setOnClickListener {
                        // 개별 마커 선택 해제
                        selectedMarker?.let { marker ->
                            animateMarkerSelection(marker, false)
                            marker.zIndex = 0
                        }
                        selectedMarker = null
                        selectedContent = null

                        // 이전 선택된 클러스터 해제
                        selectedClusterMarker?.let { oldCluster ->
                            val oldCount = oldCluster.captionText.replace("개 항목", "").toIntOrNull() ?: 1
                            oldCluster.icon = getClusterIcon(oldCount, false)
                            oldCluster.zIndex = 0
                        }
                        
                        // 새로운 클러스터 선택
                        selectedClusterMarker = this
                        this.icon = getClusterIcon(cluster.size, true)
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
        // QuadTree 생성 (데이터가 변경된 경우에만)
        if (quadTree == null) {
            quadTree = QuadTree.fromMapContents(mapContents)
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
            naverMap.cameraPosition.zoom >= 21 -> 1.0    // 1m - 최대 줌 (거의 동일한 위치만)
            naverMap.cameraPosition.zoom >= 20 -> 2.0    // 2m - 초초세밀
            naverMap.cameraPosition.zoom >= 19 -> 3.0    // 3m - 초세밀
            naverMap.cameraPosition.zoom >= 18 -> 5.0    // 5m - 매우 세밀
            naverMap.cameraPosition.zoom >= 17 -> 8.0    // 8m - 세밀
            naverMap.cameraPosition.zoom >= 16 -> 12.0   // 12m
            naverMap.cameraPosition.zoom >= 15 -> 18.0   // 18m
            naverMap.cameraPosition.zoom >= 14 -> 28.0   // 28m
            naverMap.cameraPosition.zoom >= 13 -> 45.0   // 45m
            naverMap.cameraPosition.zoom >= 12 -> 75.0   // 75m
            naverMap.cameraPosition.zoom >= 11 -> 130.0  // 130m
            naverMap.cameraPosition.zoom >= 10 -> 220.0  // 220m
            else -> 450.0 // 450m - 멀리서 볼 때는 넓게 클러스터링
        }
    }

    private fun clearAllMarkers() {
        markers.forEach { it.map = null }
        markers.clear()

        clusterMarkers.forEach { it.map = null }
        clusterMarkers.clear()

        // QuadTree도 초기화 (새로운 데이터로 다시 생성하기 위해)
        quadTree = null

        // 선택 상태는 유지 (selectedMarker, selectedContent는 그대로)
        // 단, 클러스터는 새로 생성되므로 참조 초기화
        selectedClusterMarker = null
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

    // 마커 선택/해제 애니메이션 - ObjectAnimator + 캐시 사용으로 최적화
    private fun animateMarkerSelection(marker: Marker, isSelected: Boolean) {
        val content = marker.tag as? MapContent
        
        if (isSelected) {
            // 선택 시: 캐시된 아이콘 사용 + 크기 애니메이션
            marker.icon = getSelectedMarkerIcon(content?.postType)
            
            // ObjectAnimator로 크기 애니메이션 (GPU 가속)
            val scaleAnimator = ObjectAnimator.ofFloat(marker, "alpha", 0.7f, 1.0f)
            scaleAnimator.duration = 200
            scaleAnimator.interpolator = android.view.animation.OvershootInterpolator(1.2f)
            scaleAnimator.start()
            
        } else {
            // 해제 시: 부드러운 축소 + 캐시된 아이콘 변경
            val scaleAnimator = ObjectAnimator.ofFloat(marker, "alpha", 1.0f, 0.8f, 1.0f)
            scaleAnimator.duration = 150
            scaleAnimator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            scaleAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    marker.icon = getNormalMarkerIcon(content?.postType)
                }
            })
            
            scaleAnimator.start()
        }
    }

    // 마커 포커스 애니메이션 (중앙 근처 마커) - ObjectAnimator + 캐시 최적화
    private fun animateMarkerFocus(marker: Marker, isFocused: Boolean) {
        val content = marker.tag as? MapContent
        
        if (isFocused) {
            // 포커스 시: 캐시된 아이콘 사용 + 부드러운 펄스 효과
            marker.icon = getHighlightedMarkerIcon(content?.postType)
            
            // ObjectAnimator로 펄스 애니메이션
            val pulseAnimator = ObjectAnimator.ofFloat(marker, "alpha", 0.6f, 1.0f, 0.8f, 1.0f)
            pulseAnimator.duration = 300
            pulseAnimator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            pulseAnimator.start()
            
        } else {
            // 포커스 해제 시: 페이드 아웃 후 캐시된 아이콘 변경
            val fadeAnimator = ObjectAnimator.ofFloat(marker, "alpha", 1.0f, 0.7f, 1.0f)
            fadeAnimator.duration = 200
            fadeAnimator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            fadeAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    marker.icon = getNormalMarkerIcon(content?.postType)
                }
            })
            
            fadeAnimator.start()
        }
    }

    private fun createClusterIcon(count: Int, isSelected: Boolean = false): OverlayImage {
        val size = if (isSelected) 96 else 80 // 선택 시 크기 증가
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
        val size = (64 * scale).toInt() // 스케일 적용
        val bitmap = Bitmap.createBitmap(size, (size * 1.125).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawable?.setBounds(0, 0, size, (size * 1.125).toInt())
        drawable?.draw(canvas)
        
        return OverlayImage.fromBitmap(bitmap)
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
    
    private fun getClusterIcon(count: Int, isSelected: Boolean): OverlayImage {
        val key = if (isSelected) "selected_$count" else "normal_$count"
        return clusterIconCache[key] ?: createClusterIconInternal(count, isSelected).also {
            clusterIconCache[key] = it
        }
    }

    // 내부 아이콘 생성 함수들 - 캐시 미스시에만 호출
    private fun createNormalMarkerIconInternal(postType: String?): OverlayImage {
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

    private fun createSelectedMarkerIconInternal(postType: String?): OverlayImage {
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

    private fun createHighlightedMarkerIconInternal(postType: String?): OverlayImage {
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
    
    private fun createClusterIconInternal(count: Int, isSelected: Boolean): OverlayImage {
        val size = if (isSelected) 96 else 80 // 선택 시 크기 증가
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
    
    // 툴팁 뷰 생성 함수들 제거됨 - Compose 툴팁 사용
}