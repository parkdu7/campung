package com.shinhan.campung.presentation.ui.components.bottomsheet

/**
 * 바텀시트의 Y 위치값들을 관리하는 데이터 클래스
 */
internal data class BottomSheetPositions(
    val hidden: Float,
    val partiallyExpanded: Float,
    val expanded: Float
) {
    /**
     * BottomSheetValue에 해당하는 Y 위치를 반환
     */
    fun getPositionFor(value: BottomSheetValue): Float {
        return when (value) {
            BottomSheetValue.Hidden -> hidden
            BottomSheetValue.PartiallyExpanded -> partiallyExpanded
            BottomSheetValue.Expanded -> expanded
        }
    }
    
    /**
     * 주어진 Y 위치에 가장 가까운 BottomSheetValue를 반환
     */
    fun getClosestValue(position: Float): BottomSheetValue {
        val distanceToHidden = kotlin.math.abs(position - hidden)
        val distanceToPartiallyExpanded = kotlin.math.abs(position - partiallyExpanded)
        val distanceToExpanded = kotlin.math.abs(position - expanded)
        
        return when {
            distanceToHidden <= distanceToPartiallyExpanded && distanceToHidden <= distanceToExpanded -> BottomSheetValue.Hidden
            distanceToPartiallyExpanded <= distanceToExpanded -> BottomSheetValue.PartiallyExpanded
            else -> BottomSheetValue.Expanded
        }
    }
}