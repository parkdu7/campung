package com.shinhan.campung.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun AnimatedMapTooltip(
    visible: Boolean,
    content: String,
    position: Offset,
    type: TooltipType,
    modifier: Modifier = Modifier
) {
    // 애니메이션 디버깅용 로그
    androidx.compose.runtime.LaunchedEffect(visible) {
        android.util.Log.d("AnimatedMapTooltip", "애니메이션 상태 변화: visible=$visible, content=$content, position=$position")
    }
    
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (position.x - 60).roundToInt(), // 툴팁 중앙 정렬을 위한 오프셋  
                    y = (position.y - 140).roundToInt() // 마커 위쪽 140px (더 위로)
                )
            }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(250, easing = FastOutSlowInEasing) // 빠르게 250ms
            ) + scaleIn(
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                initialScale = 0.7f // 좀 더 자연스러운 시작 스케일
            ),
            exit = fadeOut(
                animationSpec = tween(150) // 빠른 사라짐
            ) + scaleOut(
                animationSpec = tween(150),
                targetScale = 0.7f
            )
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = when (type) {
                            TooltipType.CLICK -> Color(0xE6000000) // 클릭용: 진한 검정
                            TooltipType.FOCUS -> Color(0xCC333333) // 포커스용: 연한 회색
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = content,
                    color = when (type) {
                        TooltipType.CLICK -> Color.White
                        TooltipType.FOCUS -> Color(0xFFEEEEEE)
                    },
                    fontSize = when (type) {
                        TooltipType.CLICK -> 14.sp
                        TooltipType.FOCUS -> 12.sp
                    },
                    fontWeight = when (type) {
                        TooltipType.CLICK -> FontWeight.Medium
                        TooltipType.FOCUS -> FontWeight.Normal
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}