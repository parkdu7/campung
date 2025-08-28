package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.*

@Composable
fun CustomWheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemHeight = 44.dp // iOS 스타일에 맞게 조정
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    // 선택 변경 시 햅틱 피드백
    LaunchedEffect(selectedIndex) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    // 실시간 스크롤 오프셋 계산
    val currentScrollOffset by remember {
        derivedStateOf {
            if (listState.layoutInfo.totalItemsCount == 0) selectedIndex.toFloat()
            else listState.firstVisibleItemScrollOffset.toFloat() / itemHeightPx + listState.firstVisibleItemIndex
        }
    }
    
    // 스크롤 종료 시 자동 스냅
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = currentScrollOffset.roundToInt().coerceIn(0, items.size - 1)
            
            // 부드러운 스냅 애니메이션
            if (abs(listState.firstVisibleItemScrollOffset) > 5) {
                coroutineScope.launch {
                    listState.animateScrollToItem(centerIndex)
                }
            }
            
            if (centerIndex != selectedIndex) {
                onSelectionChanged(centerIndex)
            }
        }
    }
    
    Box(
        modifier = modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // 3D 원기둥 배경 효과 (더 미묘하게)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.01f),
                            Color.Black.copy(alpha = 0.02f),
                            Color.Black.copy(alpha = 0.01f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        
        // 선택 영역 하이라이트 (iOS 스타일에 맞게 조정)
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth(0.9f)
                .background(
                    Color(0xFFE8E8E8).copy(alpha = 0.4f), // 더 연한 회색
                    RoundedCornerShape(8.dp)
                )
                .border(
                    0.5.dp, // 더 얇은 테두리
                    Color(0xFFD0D0D0).copy(alpha = 0.5f), 
                    RoundedCornerShape(8.dp)
                )
        )
        
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2))
        ) {
            itemsIndexed(items) { index, item ->
                val centerOffset = currentScrollOffset - index
                val distanceFromCenter = abs(centerOffset).toFloat()
                
                // 3D 원기둥 효과 계산
                val maxDistance = visibleItemsCount / 2f
                val normalizedDistance = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                
                // 투명도 곡선 (iOS 스타일에 맞게 조정)
                val alpha = max(0.15f, cos(normalizedDistance * PI / 2).toFloat().pow(0.8f))
                
                // 크기 곡선 (더 자연스럽게)
                val scale = max(0.65f, cos(normalizedDistance * PI / 3).toFloat().pow(0.9f))
                
                // 회전 각도 (iOS와 더 유사하게)
                val rotationX = centerOffset * 25f
                
                // 깊이감을 위한 Y축 이동
                val translationY = sin(Math.toRadians(rotationX.toDouble())).toFloat() * 10f
                
                Text(
                    text = item,
                    fontSize = (18 * scale).sp, // 크기 조정
                    fontWeight = if (normalizedDistance < 0.1f) 
                        FontWeight.SemiBold // iOS 스타일 폰트 웨이트
                    else 
                        FontWeight.Normal,
                    color = if (normalizedDistance < 0.1f)
                        Color.Black.copy(alpha = 0.9f)
                    else
                        Color.Black.copy(alpha = alpha * 0.7f),
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = alpha
                            this.scaleX = scale
                            this.scaleY = scale
                            this.rotationX = rotationX
                            this.translationY = translationY
                            
                            // 더 깊은 원근감
                            cameraDistance = 12f * density
                        }
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // iOS 스타일 상하단 페이드 효과
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

@Composable
fun SimpleKoreanDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate = LocalDate.now(),
    minDate: LocalDate = LocalDate.of(2020, 1, 1)
) {
    val today = LocalDate.now()
    
    var tempYear by remember(selectedDate) { mutableIntStateOf(selectedDate.year) }
    var tempMonth by remember(selectedDate) { mutableIntStateOf(selectedDate.monthValue) }
    var tempDay by remember(selectedDate) { mutableIntStateOf(selectedDate.dayOfMonth) }
    var isInitialized by remember { mutableStateOf(false) }
    
    val years = (minDate.year..today.year).toList()
    
    // 현재 선택된 연도에 따라 월 범위 결정
    val availableMonths = if (tempYear == today.year) {
        (1..today.monthValue).toList()
    } else {
        (1..12).toList()
    }
    
    // 현재 선택된 연월에 따라 일 범위 결정
    val availableDaysForCurrentMonth = if (tempYear == today.year && tempMonth == today.monthValue) {
        (1..today.dayOfMonth).toList()
    } else {
        val maxDays = try {
            LocalDate.of(tempYear, tempMonth, 1).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
        (1..maxDays).toList()
    }
    
    // 초기화 완료 표시 - 첫 번째 컴포지션 후 즉시 설정
    LaunchedEffect(tempYear, tempMonth, tempDay) {
        isInitialized = true
    }
    
    // 사용자 조작에 의한 날짜 범위 변경 시에만 자동 조정
    LaunchedEffect(tempYear, tempMonth) {
        if (isInitialized) { // 초기화 완료 후에만 실행
            // 연도 변경으로 월이 범위를 벗어날 때만 조정
            if (tempYear == today.year && tempMonth > today.monthValue) {
                tempMonth = today.monthValue
            }
            // 연월 변경으로 일이 범위를 벗어날 때만 조정
            if (tempYear == today.year && tempMonth == today.monthValue && tempDay > today.dayOfMonth) {
                tempDay = today.dayOfMonth
            } else if (tempDay > availableDaysForCurrentMonth.size) {
                tempDay = availableDaysForCurrentMonth.size
            }
        }
    }
    
    fun updateDate() {
        // 초기화가 완료되지 않았으면 호출하지 않음
        if (!isInitialized) return
        
        try {
            val newDate = LocalDate.of(tempYear, tempMonth, tempDay)
            // 오늘 날짜 이후는 선택할 수 없도록 제한
            val adjustedDate = if (newDate.isAfter(maxDate)) {
                maxDate
            } else if (newDate.isBefore(minDate)) {
                minDate
            } else {
                newDate
            }
            
            // 조정된 날짜로 다시 설정
            if (adjustedDate != newDate) {
                tempYear = adjustedDate.year
                tempMonth = adjustedDate.monthValue
                tempDay = adjustedDate.dayOfMonth
            }
            
            onDateSelected(adjustedDate)
        } catch (e: Exception) {
            // 에러 발생 시 오늘 날짜로 설정
            val today = LocalDate.now()
            tempYear = today.year
            tempMonth = today.monthValue
            tempDay = today.dayOfMonth
            onDateSelected(today)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "날짜를 선택해 주세요",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 연도 휠
            CustomWheelPicker(
                items = years.map { "${it}년" },
                selectedIndex = years.indexOf(tempYear).coerceAtLeast(0),
                onSelectionChanged = { index ->
                    tempYear = years[index]
                    updateDate()
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 월 휠
            CustomWheelPicker(
                items = availableMonths.map { "${it}월" },
                selectedIndex = availableMonths.indexOf(tempMonth).coerceAtLeast(0),
                onSelectionChanged = { index ->
                    tempMonth = availableMonths[index]
                    updateDate()
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 일 휠 (동적으로 업데이트)
            CustomWheelPicker(
                items = availableDaysForCurrentMonth.map { "${it}일" },
                selectedIndex = availableDaysForCurrentMonth.indexOf(tempDay).coerceAtLeast(0),
                onSelectionChanged = { index ->
                    tempDay = availableDaysForCurrentMonth[index]
                    updateDate()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KoreanDatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate = LocalDate.now(),
    minDate: LocalDate = LocalDate.of(2020, 1, 1)
) {
    var tempDate by remember(selectedDate) { mutableStateOf(selectedDate) }
    
    Dialog(
        onDismissRequest = onDismiss // 바깥 클릭 시 닫기 허용
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(48.dp)) // iOS 스타일 라운드
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            SimpleKoreanDatePicker(
                selectedDate = tempDate,
                onDateSelected = { tempDate = it },
                maxDate = maxDate,
                minDate = minDate,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 텍스트 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            "취소",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    
                    TextButton(
                        onClick = { 
                            onDateSelected(tempDate)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "저장",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KoreanDatePicker(
    selectedDate: LocalDate = LocalDate.now(),
    onDateSelected: (LocalDate) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    KoreanDatePickerDialog(
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onDismiss = onDismiss
    )
}

