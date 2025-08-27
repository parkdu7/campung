package com.shinhan.campung.presentation.ui.map

import com.shinhan.campung.data.model.MapContent
import kotlin.math.*

data class Bounds(
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double
) {
    fun contains(lat: Double, lng: Double): Boolean {
        return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng
    }
    
    fun intersects(other: Bounds): Boolean {
        return !(other.maxLat < minLat || other.minLat > maxLat || 
                other.maxLng < minLng || other.minLng > maxLng)
    }
}

class QuadTreeNode(
    val bounds: Bounds,
    val maxCapacity: Int = 10
) {
    private val points = mutableListOf<MapContent>()
    private var children: Array<QuadTreeNode>? = null
    private var isDivided = false
    
    fun insert(point: MapContent): Boolean {
        if (!bounds.contains(point.location.latitude, point.location.longitude)) {
            return false
        }
        
        if (points.size < maxCapacity && !isDivided) {
            points.add(point)
            return true
        }
        
        if (!isDivided) {
            subdivide()
        }
        
        children?.forEach { child ->
            if (child.insert(point)) {
                return true
            }
        }
        
        return false
    }
    
    private fun subdivide() {
        val centerLat = (bounds.minLat + bounds.maxLat) / 2
        val centerLng = (bounds.minLng + bounds.maxLng) / 2
        
        children = arrayOf(
            // 북서
            QuadTreeNode(Bounds(centerLat, bounds.minLng, bounds.maxLat, centerLng), maxCapacity),
            // 북동
            QuadTreeNode(Bounds(centerLat, centerLng, bounds.maxLat, bounds.maxLng), maxCapacity),
            // 남서
            QuadTreeNode(Bounds(bounds.minLat, bounds.minLng, centerLat, centerLng), maxCapacity),
            // 남동
            QuadTreeNode(Bounds(bounds.minLat, centerLng, centerLat, bounds.maxLng), maxCapacity)
        )
        
        // 기존 포인트들을 자식 노드로 재배치
        val pointsToRelocate = points.toList()
        points.clear()
        
        pointsToRelocate.forEach { point ->
            children?.forEach { child ->
                if (child.insert(point)) {
                    return@forEach
                }
            }
        }
        
        isDivided = true
    }
    
    fun queryRange(range: Bounds): List<MapContent> {
        val result = mutableListOf<MapContent>()
        
        if (!bounds.intersects(range)) {
            return result
        }
        
        // 현재 노드의 포인트들 확인
        points.forEach { point ->
            if (range.contains(point.location.latitude, point.location.longitude)) {
                result.add(point)
            }
        }
        
        // 자식 노드들 확인
        if (isDivided) {
            children?.forEach { child ->
                result.addAll(child.queryRange(range))
            }
        }
        
        return result
    }
    
    fun queryRadius(centerLat: Double, centerLng: Double, radiusMeters: Double): List<MapContent> {
        val result = mutableListOf<MapContent>()
        
        // 원을 포함하는 사각형 범위 계산
        val latDelta = radiusMeters / 111000.0 // 대략적인 위도 1도 = 111km
        val lngDelta = radiusMeters / (111000.0 * cos(Math.toRadians(centerLat)))
        
        val searchBounds = Bounds(
            centerLat - latDelta,
            centerLng - lngDelta,
            centerLat + latDelta,
            centerLng + lngDelta
        )
        
        // 사각형 범위 내의 모든 포인트 찾기
        val candidatePoints = queryRange(searchBounds)
        
        // 실제 거리로 필터링
        candidatePoints.forEach { point ->
            val distance = calculateDistance(
                centerLat, centerLng,
                point.location.latitude, point.location.longitude
            )
            if (distance <= radiusMeters) {
                result.add(point)
            }
        }
        
        return result
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
}

class QuadTree(bounds: Bounds) {
    private val root = QuadTreeNode(bounds)
    
    fun insert(point: MapContent) {
        root.insert(point)
    }
    
    fun queryRange(bounds: Bounds): List<MapContent> {
        return root.queryRange(bounds)
    }
    
    fun queryRadius(centerLat: Double, centerLng: Double, radiusMeters: Double): List<MapContent> {
        return root.queryRadius(centerLat, centerLng, radiusMeters)
    }
    
    companion object {
        fun fromMapContents(mapContents: List<MapContent>): QuadTree {
            if (mapContents.isEmpty()) {
                return QuadTree(Bounds(0.0, 0.0, 0.0, 0.0))
            }
            
            // 전체 데이터의 경계 계산
            val minLat = mapContents.minOf { it.location.latitude }
            val maxLat = mapContents.maxOf { it.location.latitude }
            val minLng = mapContents.minOf { it.location.longitude }
            val maxLng = mapContents.maxOf { it.location.longitude }
            
            // 약간의 여백 추가
            val latMargin = (maxLat - minLat) * 0.1
            val lngMargin = (maxLng - minLng) * 0.1
            
            val bounds = Bounds(
                minLat - latMargin,
                minLng - lngMargin,
                maxLat + latMargin,
                maxLng + lngMargin
            )
            
            val quadTree = QuadTree(bounds)
            mapContents.forEach { quadTree.insert(it) }
            
            return quadTree
        }
    }
}