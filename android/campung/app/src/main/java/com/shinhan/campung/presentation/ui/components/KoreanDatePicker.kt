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
    visibleItemsCount: Int = 7
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
    val years = (minDate.year..maxDate.year).toList()
    val months = (1..12).toList()
    val days = (1..31).toList()
    
    var tempYear by remember { mutableIntStateOf(selectedDate.year) }
    var tempMonth by remember { mutableIntStateOf(selectedDate.monthValue) }
    var tempDay by remember { mutableIntStateOf(selectedDate.dayOfMonth) }
    
    // 동적 일수 계산
    val maxDaysInMonth = remember(tempYear, tempMonth) {
        try {
            LocalDate.of(tempYear, tempMonth, 1).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }
    
    val availableDays = (1..maxDaysInMonth).toList()
    
    // 날짜가 유효하지 않으면 조정
    LaunchedEffect(maxDaysInMonth) {
        if (tempDay > maxDaysInMonth) {
            tempDay = maxDaysInMonth
        }
    }
    
    fun updateDate() {
        try {
            val newDate = LocalDate.of(tempYear, tempMonth, tempDay)
            if (!newDate.isAfter(maxDate) && !newDate.isBefore(minDate)) {
                onDateSelected(newDate)
            }
        } catch (e: Exception) {
            // 에러 무시
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
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 연도 휠
            CustomWheelPicker(
                items = years.map { "${it}년" },
                selectedIndex = (tempYear - years.first()).coerceIn(0, years.size - 1),
                onSelectionChanged = { index ->
                    tempYear = years[index]
                    updateDate()
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 월 휠
            CustomWheelPicker(
                items = months.map { "${it}월" },
                selectedIndex = (tempMonth - 1).coerceIn(0, months.size - 1),
                onSelectionChanged = { index ->
                    tempMonth = months[index]
                    updateDate()
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 일 휠 (동적으로 업데이트)
            CustomWheelPicker(
                items = availableDays.map { "${it}일" },
                selectedIndex = (tempDay - 1).coerceIn(0, availableDays.size - 1),
                onSelectionChanged = { index ->
                    tempDay = availableDays[index]
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
                .background(Color.White, RoundedCornerShape(20.dp)) // iOS 스타일 라운드
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 핸들 바 (iOS 스타일)
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        Color(0xFFD1D1D6),
                        RoundedCornerShape(2.dp)
                    )
                    .padding(bottom = 16.dp)
            )
            
            SimpleKoreanDatePicker(
                selectedDate = tempDate,
                onDateSelected = { tempDate = it },
                maxDate = maxDate,
                minDate = minDate,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // iOS 스타일 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "취소",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                Button(
                    onClick = { 
                        onDateSelected(tempDate)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "저장",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KoreanDatePicker(
    onDateSelected: (LocalDate) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    KoreanDatePickerDialog(
        selectedDate = LocalDate.now(),
        onDateSelected = onDateSelected,
        onDismiss = onDismiss
    )
}

// 사용 예제
@Composable
fun DatePickerExample() {
    var showDatePicker by remember { mutableStateOf(false) }
    val currentDate = LocalDate.now()
    var selectedDate by remember { 
        mutableStateOf("${currentDate.year}년 ${currentDate.monthValue}월 ${currentDate.dayOfMonth}일") 
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { showDatePicker = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "날짜 선택하기",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = selectedDate,
            modifier = Modifier.padding(top = 16.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )
    }
    
    if (showDatePicker) {
        KoreanDatePickerDialog(
            selectedDate = LocalDate.now(),
            onDateSelected = { date ->
                selectedDate = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일"
            },
            onDismiss = { 
                showDatePicker = false 
            }
        )
    }
}