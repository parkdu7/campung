package com.shinhan.campung.presentation.ui.map

/**
 * 마커 크기 중앙 관리 설정
 * 모든 마커 크기는 여기서만 수정하면 됨
 */
object MarkerConfig {
    
    // 기본 마커 크기 (픽셀)
    const val BASE_MARKER_SIZE = 135
    
    // 스케일 배율
    const val SELECTED_SCALE = 1.5f
    const val HIGHLIGHTED_SCALE = 1.4f
    
    // 계산된 크기들 (편의용)
    val SELECTED_SIZE: Int get() = (BASE_MARKER_SIZE * SELECTED_SCALE).toInt()
    val HIGHLIGHTED_SIZE: Int get() = (BASE_MARKER_SIZE * HIGHLIGHTED_SCALE).toInt()
    
    // POI 마커 크기 (일반적으로 동일하게 설정)
    val POI_MARKER_SIZE: Int get() = BASE_MARKER_SIZE
    
    // 녹음 마커 크기 (일반 마커보다 작게 - 약 20% 작음)
    const val RECORD_MARKER_SIZE = 108  // 135 * 0.8 = 108
    const val RECORD_SELECTED_SCALE = 1.4f  // 녹음 마커는 선택 시 적당히 커지도록
    val RECORD_SELECTED_SIZE: Int get() = (RECORD_MARKER_SIZE * RECORD_SELECTED_SCALE).toInt()
    
    // 클러스터 크기
    const val CLUSTER_BASE_SIZE = 80
    const val CLUSTER_SELECTED_SIZE = 96
}