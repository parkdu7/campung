package com.shinhan.campung.presentation.ui.components.bottomsheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

/**
 * 바텀시트의 상태를 관리하는 클래스
 */
@Stable
class BottomSheetState(
    initialValue: BottomSheetValue = BottomSheetValue.PartiallyExpanded,
    val confirmValueChange: (BottomSheetValue) -> Boolean = { true }
) {
    /**
     * 현재 바텀시트의 상태값
     */
    var currentValue: BottomSheetValue by mutableStateOf(initialValue)
        private set

    /**
     * 현재 바텀시트가 드래그 중인지 여부
     */
    var isDragging: Boolean by mutableStateOf(false)
        internal set

    /**
     * 바텀시트 위치 정보
     */
    internal var positions: BottomSheetPositions? by mutableStateOf(null)

    /**
     * 애니메이션 객체
     */
    internal val animatable = Animatable(0f)

    /**
     * 현재 Y 오프셋 (픽셀 단위)
     */
    val offsetY: Float
        get() = animatable.value

    /**
     * 애니메이션 진행률 (0f = PartiallyExpanded, 1f = Expanded)
     */
    val progress: Float by derivedStateOf {
        positions?.let { pos ->
            if (pos.partiallyExpanded == pos.expanded) return@let 0f
            val currentOffset = offsetY
            ((pos.partiallyExpanded - currentOffset) / (pos.partiallyExpanded - pos.expanded))
                .coerceIn(0f, 1f)
        } ?: 0f
    }

    /**
     * 지정된 상태로 애니메이션과 함께 이동
     */
    suspend fun animateTo(
        targetValue: BottomSheetValue,
        animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) {
        if (!confirmValueChange(targetValue)) return

        positions?.let { pos ->
            val targetPosition = pos.getPositionFor(targetValue)
            try {
                animatable.animateTo(targetPosition, animationSpec)
                currentValue = targetValue
            } catch (cancellationException: CancellationException) {
                // 애니메이션이 취소된 경우 현재 위치에 가장 가까운 상태로 설정
                val closestValue = pos.getClosestValue(animatable.value)
                if (confirmValueChange(closestValue)) {
                    currentValue = closestValue
                }
                throw cancellationException
            }
        }
    }

    /**
     * 즉시 지정된 상태로 이동 (애니메이션 없음)
     */
    suspend fun snapTo(targetValue: BottomSheetValue) {
        if (!confirmValueChange(targetValue)) return

        positions?.let { pos ->
            val targetPosition = pos.getPositionFor(targetValue)
            animatable.snapTo(targetPosition)
            currentValue = targetValue
        }
    }

    /**
     * 드래그 중 실시간으로 위치 업데이트
     */
    internal suspend fun updateOffset(newOffset: Float) {
        positions?.let { pos ->
            val constrainedOffset = newOffset.coerceIn(pos.expanded, pos.partiallyExpanded)
            animatable.snapTo(constrainedOffset)
        }
    }

    /**
     * 드래그 종료 시 적절한 상태로 스냅
     */
    internal suspend fun settle(velocity: Float) {
        positions?.let { pos ->
            val fastSwipeThreshold = 800f
            val midpoint = (pos.partiallyExpanded + pos.expanded) / 2

            val targetState = when {
                velocity > fastSwipeThreshold -> BottomSheetValue.PartiallyExpanded
                velocity < -fastSwipeThreshold -> BottomSheetValue.Expanded
                animatable.value > midpoint -> BottomSheetValue.PartiallyExpanded
                else -> BottomSheetValue.Expanded
            }

            if (confirmValueChange(targetState)) {
                animateTo(targetState)
            }
        }
    }

    companion object {
        /**
         * BottomSheetState를 저장/복원하기 위한 Saver
         */
        fun Saver(
            confirmValueChange: (BottomSheetValue) -> Boolean
        ): Saver<BottomSheetState, BottomSheetValue> = Saver(
            save = { it.currentValue },
            restore = { BottomSheetState(it, confirmValueChange) }
        )
    }
}

/**
 * BottomSheetState를 생성하고 기억하는 Composable 함수
 */
@Composable
fun rememberBottomSheetState(
    initialValue: BottomSheetValue = BottomSheetValue.PartiallyExpanded,
    confirmValueChange: (BottomSheetValue) -> Boolean = { true }
): BottomSheetState {
    return rememberSaveable(
        saver = BottomSheetState.Saver(confirmValueChange)
    ) {
        BottomSheetState(initialValue, confirmValueChange)
    }
}