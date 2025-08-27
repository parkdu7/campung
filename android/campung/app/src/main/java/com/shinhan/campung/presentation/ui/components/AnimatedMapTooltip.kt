package com.shinhan.campung.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

// 기존 enum 그대로 사용한다고 가정
// enum class TooltipType { CLICK, FOCUS }

@Composable
fun AnimatedMapTooltip(
    visible: Boolean,
    content: String,
    position: Offset,
    type: TooltipType,
    modifier: Modifier = Modifier
) {
    // ===== 0) 글라스 배경용 미세 애니메이션 (그대로 유지) =====
    val infiniteTransition = rememberInfiniteTransition(label = "glass_animation")
    val glassShimmer by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    val backgroundShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_shift"
    )

    // ===== 1) exit 중 내용이 사라지지 않도록 마지막 내용 보존 =====
    var lastContent by remember { mutableStateOf(content) }
    LaunchedEffect(content) {
        if (content.isNotBlank()) lastContent = content
    }

    // ===== 2) 최초/토글 애니메이션을 항상 보장 =====
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) {
        transitionState.targetState = visible
    }

    // ===== 3) 다음 마커 선택될 때만 팝 애니메이션 재생 =====
    val overshootEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)
    val bounce = remember { Animatable(1f) } // 스케일 재팝
    val microFade = remember { Animatable(1f) } // 미세 페이드

    // 마지막으로 애니메이션을 재생한 content를 기억
    var lastAnimatedContent by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(visible, content) {
        if (visible && content != lastAnimatedContent) {
            // 새로운 마커가 선택되었을 때만 애니메이션 재생
            bounce.snapTo(0.85f)
            microFade.snapTo(0.95f)
            bounce.animateTo(1f, tween(400, easing = overshootEasing))
            microFade.animateTo(1f, tween(220))
            lastAnimatedContent = content
        }
    }

    // ===== 4) 툴팁 배치 (기존과 동일한 오프셋 상수 유지) =====
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (position.x - 100).roundToInt(),
                    y = (position.y - 200).roundToInt()
                )
            }
            .zIndex(10f)
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            // ⚠️ key(...) 절대 사용하지 않음
            enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.3f,
                        animationSpec = tween(400, easing = overshootEasing)
                    ),
            exit = fadeOut(animationSpec = tween(180)) +
                    scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ),
            modifier = Modifier.graphicsLayer {
                // 꼬리(아래 중앙)를 기준으로 스케일/페이드
                transformOrigin = TransformOrigin(0.5f, 1f)
                scaleX = bounce.value
                scaleY = bounce.value
                alpha = microFade.value
            }
        ) {
            // ===== 5) 말풍선 UI (당신이 만든 글라스 스타일 유지) =====
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 메인 글라스 박스
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = Color.Black.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 백드롭 블러 느낌의 복합 레이어(단순화된 안전 버전)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        // 다중 레이어 흐림 느낌
                        repeat(6) { idx ->
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0x22FFFFFF),
                                                Color(0x14F5F5F5),
                                                Color(0x10E8E8E8)
                                            ),
                                            radius = (120f + (idx * 25f)) * glassShimmer,
                                            center = Offset(
                                                (0.2f + (idx * 0.08f) + (backgroundShift * 0.1f)).coerceIn(0f, 1f),
                                                (0.3f + (idx * 0.06f) + (backgroundShift * 0.05f)).coerceIn(0f, 1f)
                                            )
                                        )
                                    )
                            )
                        }

                        // 추가 디퓨즈 스윕
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0x15FFFFFF),
                                            Color(0x08F8F8F8),
                                            Color(0x15FFFFFF),
                                            Color(0x08F8F8F8)
                                        )
                                    )
                                )
                        )
                    }

                    // 메인 글라스 레이어(타입별 톤)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = when (type) {
                                    TooltipType.CLICK -> Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xC0FFFFFF),
                                            Color(0xA0F8F8F8)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, Float.POSITIVE_INFINITY)
                                    )
                                    TooltipType.FOCUS -> Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xA0FFFFFF),
                                            Color(0x80F0F0F0)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, Float.POSITIVE_INFINITY)
                                    )
                                }
                            )
                    )

                    // 하이라이트 반사
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = (0.25f * glassShimmer).coerceAtMost(0.5f)),
                                        Color.Transparent,
                                        Color.Transparent
                                    ),
                                    start = Offset(backgroundShift * 50f, 0f),
                                    end = Offset(backgroundShift * 50f, 100f)
                                )
                            )
                    )

                    // 외곽선
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x60FFFFFF),
                                        Color(0x20FFFFFF)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )

                    // 텍스트
                    Box(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lastContent,
                            color = when (type) {
                                TooltipType.CLICK -> Color(0xFF333333)
                                TooltipType.FOCUS -> Color(0xFF666666)
                            },
                            fontSize = when (type) {
                                TooltipType.CLICK -> 14.sp
                                TooltipType.FOCUS -> 12.sp
                            },
                            fontWeight = when (type) {
                                TooltipType.CLICK -> FontWeight.SemiBold
                                TooltipType.FOCUS -> FontWeight.Medium
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // 꼬리
                TooltipTail(type = type)
            }
        }
    }
}

@Composable
private fun TooltipTail(type: TooltipType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 8.dp)) {
        drawTail(type)
    }
}

private fun DrawScope.drawTail(type: TooltipType) {
    val trianglePath = Path().apply {
        moveTo(size.width / 2f, size.height)
        lineTo(0f, 0f)
        lineTo(size.width, 0f)
        close()
    }

    // 메인 꼬리
    drawPath(
        path = trianglePath,
        brush = when (type) {
            TooltipType.CLICK -> Brush.linearGradient(
                colors = listOf(
                    Color(0xC0FFFFFF),
                    Color(0xA0F8F8F8)
                ),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height)
            )
            TooltipType.FOCUS -> Brush.linearGradient(
                colors = listOf(
                    Color(0xA0FFFFFF),
                    Color(0x80F0F0F0)
                ),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height)
            )
        }
    )

    // 왼쪽 하이라이트 반
    val highlightPath = Path().apply {
        moveTo(size.width / 2f, size.height)
        lineTo(0f, 0f)
        lineTo(size.width / 2f, 0f)
        close()
    }
    drawPath(
        path = highlightPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x30FFFFFF),
                Color.Transparent
            ),
            start = Offset(size.width / 4f, 0f),
            end = Offset(size.width / 4f, size.height)
        )
    )
}
