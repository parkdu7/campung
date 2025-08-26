package com.shinhan.campung.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    // 디버깅 로그 제거됨
    
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (position.x - 100).roundToInt(), // 툴팁 중앙 정렬을 위한 오프셋
                    y = (position.y - 200).roundToInt() // 마커 위쪽 140px (더 위로)
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
            // 말풍선 모양 툴팁 (Box + 꼬리)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 메인 툴팁 박스
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f), // 주변 그림자 투명도 낮춤
                            spotColor = Color.Black.copy(alpha = 0.1f) // 스팟 그림자 투명도 낮춤
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
                
                // 말풍선 꼬리 (삼각형)
                Canvas(
                    modifier = Modifier.size(width = 16.dp, height = 8.dp)
                ) {
                    val trianglePath = Path().apply {
                        moveTo(size.width / 2f, size.height) // 아래 끝점
                        lineTo(0f, 0f) // 왼쪽 위
                        lineTo(size.width, 0f) // 오른쪽 위
                        close()
                    }
                    
                    drawPath(
                        path = trianglePath,
                        color = when (type) {
                            TooltipType.CLICK -> Color(0xE6000000)
                            TooltipType.FOCUS -> Color(0xCC333333)
                        }
                    )
                }
            }
        }
    }
}