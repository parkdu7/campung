package com.shinhan.campung.presentation.ui.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 지도 화면에 특화된 드래그 가능한 바텀시트 컴포넌트
 * FullMapScreen의 기존 로직을 완전히 동일하게 구현
 */
@Composable
fun MapDraggableBottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    screenHeight: Dp,
    availableHeight: Dp,
    dragHandleHeight: Dp = 30.dp,
    containerColor: Color = Color.White,
    contentColor: Color = Color.Black,
    shadowElevation: Dp = 8.dp,
    content: @Composable BottomSheetScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // FullMapScreen과 동일한 위치 계산 로직
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val availableHeightPx = with(density) { availableHeight.toPx() }

    // 바텀시트 위치 업데이트 (기존 FullMapScreen 로직과 동일)
    LaunchedEffect(availableHeightPx, screenHeightPx) {
        val positions = BottomSheetPositions(
            hidden = screenHeightPx,
            partiallyExpanded = availableHeightPx - with(density) { dragHandleHeight.toPx() },
            expanded = availableHeightPx * 0.2f // 기본값, 실제로는 컨텐츠에 따라 동적 계산
        )
        
        state.positions = positions
        
        // 초기 위치 설정
        if (state.animatable.value == 0f) {
            state.animatable.snapTo(positions.getPositionFor(state.currentValue))
        }
    }

    // 현재 오프셋
    val offsetY = state.animatable.value
    val offsetYDp = with(density) { offsetY.toDp() }

    // BottomSheetScope 생성
    val scope = remember(state) { BottomSheetScopeImpl(state) }

    // 바텀시트 배경 (사용가능한 높이까지만 연장, 상단만 radius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(availableHeight)
            .offset(y = offsetYDp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
    ) {
        // 실제 바텀시트 컨텐츠
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = shadowElevation,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        ) {
            Column {
                // 드래그 핸들 - FullMapScreen과 동일한 로직
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dragHandleHeight)
                        .pointerInput(Unit) {
                            var totalDragAmount = 0f
                            var dragStartTime = 0L

                            detectDragGestures(
                                onDragStart = {
                                    state.isDragging = true
                                    totalDragAmount = 0f
                                    dragStartTime = System.currentTimeMillis()
                                },
                                onDragEnd = {
                                    state.isDragging = false
                                    val duration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (duration > 0) {
                                        totalDragAmount / duration * 1000
                                    } else {
                                        0f
                                    }
                                    
                                    coroutineScope.launch {
                                        state.settle(velocity)
                                    }
                                }
                            ) { _, dragAmount ->
                                totalDragAmount += dragAmount.y

                                // 드래그 중 실시간 업데이트
                                coroutineScope.launch {
                                    val newY = state.animatable.value + dragAmount.y
                                    state.updateOffset(newY)
                                }
                            }
                        }
                ) {
                    DefaultDragHandle()
                }

                // 바텀시트 메인 컨텐츠
                scope.content()
            }
        }
    }
}

/**
 * 동적 컨텐츠 높이를 지원하는 MapDraggableBottomSheet
 */
@Composable
fun MapDraggableBottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    screenHeight: Dp,
    availableHeight: Dp,
    contentHeight: Dp, // 동적 컨텐츠 높이
    dragHandleHeight: Dp = 30.dp,
    containerColor: Color = Color.White,
    contentColor: Color = Color.Black,
    shadowElevation: Dp = 8.dp,
    content: @Composable BottomSheetScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val screenHeightPx = with(density) { screenHeight.toPx() }
    val availableHeightPx = with(density) { availableHeight.toPx() }

    // 컨텐츠 높이가 변경될 때마다 위치 재계산
    LaunchedEffect(availableHeightPx, contentHeight) {
        val contentHeightPx = with(density) { contentHeight.toPx() }
        val calculatedExpanded = availableHeightPx - contentHeightPx
        val partiallyExpanded = availableHeightPx - with(density) { dragHandleHeight.toPx() }
        
        val positions = BottomSheetPositions(
            hidden = screenHeightPx,
            partiallyExpanded = partiallyExpanded,
            expanded = calculatedExpanded.coerceAtMost(partiallyExpanded - with(density) { 10.dp.toPx() })
        )
        
        state.positions = positions
        
        // 현재 상태에 맞는 위치로 애니메이션
        state.animateTo(state.currentValue)
    }

    // 현재 오프셋
    val offsetY = state.animatable.value
    val offsetYDp = with(density) { offsetY.toDp() }

    // BottomSheetScope 생성
    val scope = remember(state) { BottomSheetScopeImpl(state) }

    // 바텀시트 UI (FullMapScreen과 동일한 구조)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(availableHeight)
            .offset(y = offsetYDp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = shadowElevation,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dragHandleHeight)
                        .pointerInput(Unit) {
                            var totalDragAmount = 0f
                            var dragStartTime = 0L

                            detectDragGestures(
                                onDragStart = {
                                    state.isDragging = true
                                    totalDragAmount = 0f
                                    dragStartTime = System.currentTimeMillis()
                                },
                                onDragEnd = {
                                    state.isDragging = false
                                    val duration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (duration > 0) {
                                        totalDragAmount / duration * 1000
                                    } else {
                                        0f
                                    }
                                    
                                    coroutineScope.launch {
                                        state.settle(velocity)
                                    }
                                }
                            ) { _, dragAmount ->
                                totalDragAmount += dragAmount.y

                                coroutineScope.launch {
                                    val newY = state.animatable.value + dragAmount.y
                                    state.updateOffset(newY)
                                }
                            }
                        }
                ) {
                    DefaultDragHandle()
                }

                scope.content()
            }
        }
    }
}