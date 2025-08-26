package com.shinhan.campung.presentation.ui.components.bottomsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable

/**
 * 바텀시트 컨텐츠가 사용할 수 있는 스코프 인터페이스
 */
@Stable
interface BottomSheetScope {
    /**
     * 현재 바텀시트의 상태값
     */
    val currentValue: BottomSheetValue

    /**
     * 애니메이션 진행률 (0f = PartiallyExpanded, 1f = Expanded)
     */
    val progress: Float

    /**
     * 현재 바텀시트가 드래그 중인지 여부
     */
    val isDragging: Boolean

    /**
     * 지정된 상태로 애니메이션과 함께 이동
     */
    suspend fun animateTo(
        targetValue: BottomSheetValue,
        animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
}

/**
 * BottomSheetScope의 구현체
 */
internal class BottomSheetScopeImpl(
    private val state: BottomSheetState
) : BottomSheetScope {
    
    override val currentValue: BottomSheetValue
        get() = state.currentValue

    override val progress: Float
        get() = state.progress

    override val isDragging: Boolean
        get() = state.isDragging

    override suspend fun animateTo(
        targetValue: BottomSheetValue,
        animationSpec: AnimationSpec<Float>
    ) {
        state.animateTo(targetValue, animationSpec)
    }
}