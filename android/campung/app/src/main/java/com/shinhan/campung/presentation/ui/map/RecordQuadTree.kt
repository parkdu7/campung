package com.shinhan.campung.presentation.ui.map

import com.shinhan.campung.data.model.MapRecord
import kotlin.math.*

class RecordQuadTreeNode(
    val bounds: Bounds,
    val maxCapacity: Int = 10,
    val depth: Int = 0
) {
    private val points = mutableListOf<MapRecord>()
    private var children: Array<RecordQuadTreeNode>? = null
    private var isDivided = false
    
    companion object {
        private const val MAX_DEPTH = 8
        private const val MIN_BOUNDS_SIZE = 0.0001
    }
    
    fun insert(point: MapRecord): Boolean {
        if (!bounds.contains(point.location.latitude, point.location.longitude)) {
            return false
        }
        
        if (points.size < maxCapacity && !isDivided) {
            points.add(point)
            return true
        }
        
        if (!isDivided && canSubdivide()) {
            subdivide()
        }
        
        if (isDivided) {
            children?.forEach { child ->
                if (child.insert(point)) {
                    return true
                }
            }
        }
        
        points.add(point)
        return true
    }
    
    private fun canSubdivide(): Boolean {
        if (depth >= MAX_DEPTH) {
            return false
        }
        
        val latSize = bounds.maxLat - bounds.minLat
        val lngSize = bounds.maxLng - bounds.minLng
        
        return latSize > MIN_BOUNDS_SIZE && lngSize > MIN_BOUNDS_SIZE
    }
    
    private fun subdivide() {
        val centerLat = (bounds.minLat + bounds.maxLat) / 2
        val centerLng = (bounds.minLng + bounds.maxLng) / 2
        
        children = arrayOf(
            RecordQuadTreeNode(Bounds(centerLat, bounds.minLng, bounds.maxLat, centerLng), maxCapacity, depth + 1),
            RecordQuadTreeNode(Bounds(centerLat, centerLng, bounds.maxLat, bounds.maxLng), maxCapacity, depth + 1),
            RecordQuadTreeNode(Bounds(bounds.minLat, bounds.minLng, centerLat, centerLng), maxCapacity, depth + 1),
            RecordQuadTreeNode(Bounds(bounds.minLat, centerLng, centerLat, bounds.maxLng), maxCapacity, depth + 1)
        )
        
        val pointsToRelocate = points.toList()
        points.clear()
        
        pointsToRelocate.forEach { point ->
            var inserted = false
            children?.forEach { child ->
                if (!inserted && child.insert(point)) {
                    inserted = true
                }
            }
            if (!inserted) {
                points.add(point)
            }
        }
        
        isDivided = true
    }
    
    fun queryRange(range: Bounds): List<MapRecord> {
        val result = mutableListOf<MapRecord>()
        
        if (!bounds.intersects(range)) {
            return result
        }
        
        points.forEach { point ->
            if (range.contains(point.location.latitude, point.location.longitude)) {
                result.add(point)
            }
        }
        
        if (isDivided) {
            children?.forEach { child ->
                result.addAll(child.queryRange(range))
            }
        }
        
        return result
    }
    
    fun queryRadius(centerLat: Double, centerLng: Double, radiusMeters: Double): List<MapRecord> {
        val result = mutableListOf<MapRecord>()
        
        val latDelta = radiusMeters / 111000.0
        val lngDelta = radiusMeters / (111000.0 * cos(Math.toRadians(centerLat)))
        
        val searchBounds = Bounds(
            centerLat - latDelta,
            centerLng - lngDelta,
            centerLat + latDelta,
            centerLng + lngDelta
        )
        
        val candidatePoints = queryRange(searchBounds)
        
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
        val earthRadius = 6371000.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
}

class RecordQuadTree(bounds: Bounds) {
    private val root = RecordQuadTreeNode(bounds)
    
    fun insert(point: MapRecord) {
        root.insert(point)
    }
    
    fun queryRange(bounds: Bounds): List<MapRecord> {
        return root.queryRange(bounds)
    }
    
    fun queryRadius(centerLat: Double, centerLng: Double, radiusMeters: Double): List<MapRecord> {
        return root.queryRadius(centerLat, centerLng, radiusMeters)
    }
    
    companion object {
        fun fromMapRecords(mapRecords: List<MapRecord>): RecordQuadTree {
            if (mapRecords.isEmpty()) {
                return RecordQuadTree(Bounds(33.0, 124.0, 39.0, 132.0))
            }
            
            val minLat = mapRecords.minOf { it.location.latitude }
            val maxLat = mapRecords.maxOf { it.location.latitude }
            val minLng = mapRecords.minOf { it.location.longitude }
            val maxLng = mapRecords.maxOf { it.location.longitude }
            
            val latMargin = maxOf((maxLat - minLat) * 0.1, 0.01)
            val lngMargin = maxOf((maxLng - minLng) * 0.1, 0.01)
            
            val bounds = Bounds(
                minLat - latMargin,
                minLng - lngMargin,
                maxLat + latMargin,
                maxLng + lngMargin
            )
            
            val quadTree = RecordQuadTree(bounds)
            mapRecords.forEach { quadTree.insert(it) }
            
            return quadTree
        }
    }
}