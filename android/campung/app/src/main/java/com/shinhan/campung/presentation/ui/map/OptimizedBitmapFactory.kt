package com.shinhan.campung.presentation.ui.map

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.core.content.ContextCompat
import com.shinhan.campung.R
import java.util.concurrent.ConcurrentHashMap

/**
 * 비트맵 생성을 최적화하는 팩토리 클래스
 * Path 객체 사전 계산, 하드웨어 가속, 메모리 효율적 비트맵 생성
 */
class OptimizedBitmapFactory private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "OptimizedBitmapFactory"
        
        @Volatile
        private var INSTANCE: OptimizedBitmapFactory? = null
        
        fun getInstance(context: Context): OptimizedBitmapFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OptimizedBitmapFactory(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // 비트맵 설정
        private const val MARKER_BASE_SIZE = 80
        private const val CLUSTER_BASE_SIZE = 80
        private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888  // 고품질 유지
        
        // 성능 최적화 상수
        private const val ENABLE_HARDWARE_ACCELERATION = true
        private const val ENABLE_ANTI_ALIAS = true
    }
    
    // 사전 계산된 Path 캐시
    private val pathCache = ConcurrentHashMap<String, Path>()
    
    // Paint 객체 재사용
    private val reusablePaint = Paint().apply {
        isAntiAlias = ENABLE_ANTI_ALIAS
        isDither = true
        isFilterBitmap = true
    }
    
    // 클러스터용 Paint 객체들
    private val clusterFillPaint = Paint().apply {
        isAntiAlias = ENABLE_ANTI_ALIAS
        style = Paint.Style.FILL
    }
    
    private val clusterStrokePaint = Paint().apply {
        isAntiAlias = ENABLE_ANTI_ALIAS
        style = Paint.Style.STROKE
    }
    
    private val clusterTextPaint = Paint().apply {
        isAntiAlias = ENABLE_ANTI_ALIAS
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = Color.WHITE
    }
    
    init {
        Log.d(TAG, "OptimizedBitmapFactory 초기화 완료")
        preComputePaths()
    }
    
    /**
     * 마커용 비트맵 생성 (최적화된 버전)
     */
    fun createMarkerBitmap(
        postType: String?,
        scale: Float = 1.0f,
        useHardwareAcceleration: Boolean = ENABLE_HARDWARE_ACCELERATION
    ): Bitmap {
        val drawableRes = getDrawableResource(postType)
        val size = (MARKER_BASE_SIZE * scale).toInt()
        val height = (size * 1.125).toInt()
        
        val config = if (useHardwareAcceleration) BITMAP_CONFIG else Bitmap.Config.RGB_565
        val bitmap = Bitmap.createBitmap(size, height, config)
        
        val canvas = Canvas(bitmap)
        
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        drawable?.let {
            it.setBounds(0, 0, size, height)
            it.draw(canvas)
        }
        
        return bitmap
    }
    
    /**
     * 클러스터용 비트맵 생성 (사전 계산된 Path 사용)
     */
    fun createClusterBitmap(
        count: Int,
        isSelected: Boolean = false,
        useOptimizedPath: Boolean = true
    ): Bitmap {
        val size = if (isSelected) 96 else CLUSTER_BASE_SIZE
        val bitmap = Bitmap.createBitmap(size, size, BITMAP_CONFIG)
        val canvas = Canvas(bitmap)
        
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 2f
        
        if (useOptimizedPath) {
            drawClusterWithPath(canvas, centerX, centerY, radius, count, isSelected)
        } else {
            drawClusterTraditional(canvas, centerX, centerY, radius, count, isSelected)
        }
        
        return bitmap
    }
    
    /**
     * 사전 계산된 Path를 사용한 클러스터 그리기
     */
    private fun drawClusterWithPath(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        count: Int,
        isSelected: Boolean
    ) {
        // Path 캐시에서 원 Path 가져오기
        val circlePath = getOrCreateCirclePath(radius)
        
        canvas.save()
        canvas.translate(centerX, centerY)
        
        // 배경 원 그리기
        clusterFillPaint.color = if (isSelected) Color.parseColor("#FF1976D2") else Color.parseColor("#FF3F51B5")
        canvas.drawPath(circlePath, clusterFillPaint)
        
        // 테두리 그리기
        clusterStrokePaint.apply {
            color = Color.WHITE
            strokeWidth = if (isSelected) 6f else 4f
        }
        canvas.drawPath(circlePath, clusterStrokePaint)
        
        // 선택 시 추가 외곽 테두리
        if (isSelected) {
            val outerCirclePath = getOrCreateCirclePath(radius + 4f)
            clusterStrokePaint.apply {
                color = Color.parseColor("#FFE91E63")
                strokeWidth = 2f
            }
            canvas.drawPath(outerCirclePath, clusterStrokePaint)
        }
        
        canvas.restore()
        
        // 텍스트 그리기
        drawClusterText(canvas, centerX, centerY, count, isSelected)
    }
    
    /**
     * 전통적인 방식의 클러스터 그리기 (비교용)
     */
    private fun drawClusterTraditional(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        count: Int,
        isSelected: Boolean
    ) {
        // 배경 원 그리기
        clusterFillPaint.color = if (isSelected) Color.parseColor("#FF1976D2") else Color.parseColor("#FF3F51B5")
        canvas.drawCircle(centerX, centerY, radius, clusterFillPaint)
        
        // 테두리 그리기
        clusterStrokePaint.apply {
            color = Color.WHITE
            strokeWidth = if (isSelected) 6f else 4f
        }
        canvas.drawCircle(centerX, centerY, radius, clusterStrokePaint)
        
        // 선택 시 추가 외곽 테두리
        if (isSelected) {
            clusterStrokePaint.apply {
                color = Color.parseColor("#FFE91E63")
                strokeWidth = 2f
            }
            canvas.drawCircle(centerX, centerY, radius + 4f, clusterStrokePaint)
        }
        
        // 텍스트 그리기
        drawClusterText(canvas, centerX, centerY, count, isSelected)
    }
    
    /**
     * 클러스터 텍스트 그리기
     */
    private fun drawClusterText(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        count: Int,
        isSelected: Boolean
    ) {
        clusterTextPaint.textSize = when {
            count < 10 -> if (isSelected) 28f else 24f
            count < 100 -> if (isSelected) 24f else 20f
            else -> if (isSelected) 20f else 16f
        }
        
        val text = if (count > 999) "999+" else count.toString()
        val textY = centerY + clusterTextPaint.textSize / 3f
        canvas.drawText(text, centerX, textY, clusterTextPaint)
    }
    
    /**
     * Path 객체들을 사전 계산
     */
    private fun preComputePaths() {
        Log.d(TAG, "Path 객체 사전 계산 시작")
        
        // 자주 사용되는 원 크기들 미리 계산
        val commonRadii = listOf(30f, 35f, 38f, 40f, 42f, 45f, 48f, 50f)
        
        commonRadii.forEach { radius ->
            createCirclePath(radius)
        }
        
        Log.d(TAG, "Path 객체 ${pathCache.size}개 사전 계산 완료")
    }
    
    /**
     * 원형 Path 생성 또는 캐시에서 가져오기
     */
    private fun getOrCreateCirclePath(radius: Float): Path {
        val key = "circle_$radius"
        return pathCache[key] ?: createCirclePath(radius)
    }
    
    /**
     * 원형 Path 생성
     */
    private fun createCirclePath(radius: Float): Path {
        val key = "circle_$radius"
        val path = Path().apply {
            addCircle(0f, 0f, radius, Path.Direction.CW)
        }
        pathCache[key] = path
        return path
    }
    
    /**
     * PostType에 따른 Drawable 리소스 반환
     */
    private fun getDrawableResource(postType: String?): Int {
        return when(postType) {
            "NOTICE" -> R.drawable.marker_notice
            "INFO" -> R.drawable.marker_info
            "MARKET" -> R.drawable.marker_market
            "FREE" -> R.drawable.marker_free
            "HOT" -> R.drawable.marker_hot
            else -> R.drawable.marker_info
        }
    }
    
    /**
     * 메모리 정리
     */
    fun cleanup() {
        Log.d(TAG, "OptimizedBitmapFactory 정리 시작")
        pathCache.clear()
        Log.d(TAG, "OptimizedBitmapFactory 정리 완료")
    }
    
    /**
     * 팩토리 통계 정보
     */
    fun getStats(): FactoryStats {
        return FactoryStats(
            cachedPathsCount = pathCache.size,
            memoryUsageKB = estimateMemoryUsage()
        )
    }
    
    /**
     * 대략적인 메모리 사용량 계산
     */
    private fun estimateMemoryUsage(): Long {
        // Path 객체당 대략 1KB로 추정
        return pathCache.size * 1024L
    }
}

/**
 * 팩토리 통계 정보
 */
data class FactoryStats(
    val cachedPathsCount: Int,
    val memoryUsageKB: Long
) {
    override fun toString(): String {
        return "FactoryStats(pathCache: $cachedPathsCount, memory: ${memoryUsageKB}KB)"
    }
}