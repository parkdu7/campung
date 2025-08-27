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
 * 드래그 가능한 바텀시트 컴포넌트
 * 
 * @param state 바텀시트 상태 관리 객체
 * @param modifier 컴포넌트에 적용할 Modifier
 * @param containerColor 바텀시트 배경 색상
 * @param contentColor 바텀시트 컨텐츠 색상
 * @param dragHandleColor 드래그 핸들 색상
 * @param shadowElevation 그림자 높이
 * @param sheetMaxWidth 바텀시트 최대 너비
 * @param windowInsets 시스템 윈도우 인셋
 * @param peekContent 드래그 핸들 영역 컨텐츠 (기본: DefaultDragHandle)
 * @param content 바텀시트 메인 컨텐츠
 */
@Composable
fun DraggableBottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    contentColor: Color = Color.Black,
    dragHandleColor: Color = Color.Gray,
    shadowElevation: Dp = 8.dp,
    sheetMaxWidth: Dp = Dp.Unspecified,
    windowInsets: WindowInsets = WindowInsets(0),
    peekContent: @Composable () -> Unit = { 
        DefaultDragHandle(color = dragHandleColor) 
    },
    content: @Composable BottomSheetScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // WindowInsets에서 높이 정보 추출
    val bottomInset = windowInsets.getBottom(density)
    val topInset = windowInsets.getTop(density)
    
    // 화면 높이 및 사용 가능한 높이 계산
    val screenHeightPx = with(density) { 
        // 실제 화면 높이는 컴포저블이 측정된 후 알 수 있음
        // 임시로 큰 값 사용, 실제로는 BoxWithConstraints 등을 사용해야 함
        2000.dp.toPx() 
    }
    val availableHeightPx = screenHeightPx - bottomInset - topInset

    // 바텀시트 위치 계산
    LaunchedEffect(availableHeightPx) {
        // 기본 위치 계산 (FullMapScreen의 로직과 동일)
        val peekHeightPx = with(density) { 30.dp.toPx() } // 드래그 핸들 높이
        
        val positions = BottomSheetPositions(
            hidden = screenHeightPx, // 완전히 숨김
            partiallyExpanded = availableHeightPx - peekHeightPx,
            expanded = availableHeightPx * 0.2f // 기본적으로 화면의 20% 위치까지 확장
        )
        
        state.positions = positions
        
        // 초기 위치로 스냅
        if (state.animatable.value == 0f) {
            state.animatable.snapTo(positions.getPositionFor(state.currentValue))
        }
    }

    // 바텀시트 상태 변경 감지 및 애니메이션
    LaunchedEffect(state.currentValue) {
        state.positions?.let { positions ->
            val targetY = positions.getPositionFor(state.currentValue)
            if (state.animatable.value != targetY) {
                state.animateTo(state.currentValue)
            }
        }
    }

    // 현재 Y 오프셋
    val offsetY = state.animatable.value
    val offsetYDp = with(density) { offsetY.toDp() }

    // BottomSheetScope 생성
    val scope = remember(state) { BottomSheetScopeImpl(state) }

    BoxWithConstraints(modifier = modifier) {
        // 실제 화면 크기 업데이트
        LaunchedEffect(maxHeight) {
            val actualScreenHeightPx = with(density) { maxHeight.toPx() }
            val actualAvailableHeightPx = actualScreenHeightPx - bottomInset - topInset
            
            val peekHeightPx = with(density) { 30.dp.toPx() }
            
            val updatedPositions = BottomSheetPositions(
                hidden = actualScreenHeightPx,
                partiallyExpanded = actualAvailableHeightPx - peekHeightPx,
                expanded = actualAvailableHeightPx * 0.2f
            )
            
            state.positions = updatedPositions
        }

        // 바텀시트 배경 (전체 화면 높이)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = containerColor,
                contentColor = contentColor,
                shadowElevation = shadowElevation,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = sheetMaxWidth)
                        .fillMaxWidth()
                ) {
                    // 드래그 핸들 영역
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        peekContent()
                    }

                    // 바텀시트 메인 컨텐츠
                    scope.content()
                }
            }
        }
    }
}