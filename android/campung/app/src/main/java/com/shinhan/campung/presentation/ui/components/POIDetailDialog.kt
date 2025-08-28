package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.shinhan.campung.data.model.POIData
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.PI

@Composable
fun POIDetailDialog(
    poi: POIData,
    isGeneratingSummary: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 다이얼로그 등장 애니메이션
    var isVisible by remember { mutableStateOf(false) }
    
    // 등장 애니메이션 - 스케일과 알파
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialogScale"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "dialogAlpha"
    )
    
    // 다이얼로그가 나타날 때 애니메이션 시작
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    rotationZ = rotation
                    
                    // 드래그 거리에 따른 효과 계산
                    val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
                    
                    // 등장 애니메이션과 드래그 효과 결합
                    val baseAlpha = animatedAlpha
                    val baseScale = animatedScale
                    
                    if (isDragging) {
                        // 드래그 중일 때: 거리에 따라 투명해지고 작아짐
                        alpha = baseAlpha * (1f - (distance / 400f).coerceIn(0f, 0.8f))
                        val dragScaleFactor = 1f - (distance / 800f).coerceIn(0f, 0.4f)
                        scaleX = baseScale * dragScaleFactor
                        scaleY = baseScale * dragScaleFactor
                    } else {
                        // 드래그하지 않을 때: 등장 애니메이션만 적용
                        alpha = baseAlpha
                        scaleX = baseScale
                        scaleY = baseScale
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            // 360도 모든 방향으로 스와이프 거리 계산
                            val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
                            // 스와이프 거리가 충분하면 닫기 (150px 이상)
                            if (distance > 150f) {
                                coroutineScope.launch {
                                    // 닫히는 애니메이션을 위해 isVisible을 false로 변경
                                    isVisible = false
                                    // 애니메이션이 끝날 때까지 기다린 후 실제로 닫기
                                    kotlinx.coroutines.delay(200)
                                    onDismiss()
                                }
                            } else {
                                // 원래 위치로 되돌리기
                                offsetX = 0f
                                offsetY = 0f
                                rotation = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        // X, Y 두 축 모두 업데이트 (360도 자유자재로)
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        
                        // 드래그 방향에 따른 회전 각도 계산
                        val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
                        if (distance > 20f) { // 최소 거리 이상일 때만 회전 적용
                            // atan2를 사용해서 드래그 방향의 각도 계산 (라디안)
                            val angle = atan2(offsetY.toDouble(), offsetX.toDouble())
                            // 라디안을 도(degree)로 변환하고, 드래그 거리에 비례해서 회전
                            val maxRotation = 45f // 최대 회전 각도
                            val rotationFactor = (distance / 300f).coerceIn(0f, 1f) // 회전 강도
                            rotation = (angle * 180 / PI).toFloat() * rotationFactor * 0.3f // 회전을 약간 부드럽게
                            
                            // 회전 범위를 제한 (-45도 ~ +45도)
                            rotation = rotation.coerceIn(-maxRotation, maxRotation)
                        }
                    }
                },
            title = {
                Column {
                    // POI 이미지
                    poi.thumbnailUrl?.let { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "${poi.name} 이미지",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // POI 이름과 카테고리
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = poi.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 카테고리 태그
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF788CF7).copy(alpha = 0.1f),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = getCategoryDisplayName(poi.category),
                                fontSize = 12.sp,
                                color = Color(0xFF788CF7),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Current Summary (주요 정보)
                    if (isGeneratingSummary) {
                        // 로딩 상태
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF788CF7)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "상세 정보 로드 중...",
                                fontSize = 14.sp,
                                color = Color(0xFF788CF7),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // 기존 요약도 함께 표시 (있다면)
                        poi.currentSummary?.let { summary ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "기존 정보:",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summary,
                                fontSize = 14.sp,
                                color = Color.Gray.copy(alpha = 0.7f),
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // 일반 상태
                        poi.currentSummary?.let { summary ->
                            Text(
                                text = summary,
                                fontSize = 14.sp,
                                color = Color.Black,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } ?: run {
                            Text(
                                text = "이 장소에 대한 정보가 준비 중입니다.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

/**
 * 카테고리 코드를 한글 표시명으로 변환 (백엔드 LandmarkCategory enum 기준)
 */
private fun getCategoryDisplayName(category: String): String {
    return when(category.uppercase()) {
        "LIBRARY" -> "도서관"
        "RESTAURANT" -> "식당"
        "CAFE" -> "카페"
        "DORMITORY" -> "기숙사"
        "FOOD_TRUCK" -> "푸드트럭"
        "EVENT" -> "행사"
        "UNIVERSITY_BUILDING" -> "대학건물"
        else -> category // 알 수 없는 카테고리는 원본 그대로
    }
}