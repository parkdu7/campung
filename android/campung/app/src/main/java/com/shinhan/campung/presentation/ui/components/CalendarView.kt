package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.shinhan.campung.R
import com.shinhan.campung.presentation.ui.theme.CampusPrimary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.presentation.ui.theme.CampusPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun CalendarView() {
    val today = LocalDate.now()
    var selectedDay by remember { mutableStateOf(today.dayOfMonth) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 월 네비게이션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_chevron_left_24),
                        contentDescription = "이전 달"
                    )
                }
                
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_chevron_right_24),
                        contentDescription = "다음 달"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 요일 헤더
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            color = when(day) {
                                "일" -> Color.Red
                                "토" -> Color.Blue
                                else -> Color.Gray
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 캘린더 그리드
            CalendarGrid(
                currentDate = today,
                selectedDay = selectedDay
            ) { day ->
                selectedDay = day
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentDate: LocalDate,
    selectedDay: Int,
    onDayClick: (Int) -> Unit
) {
    val firstDayOfMonth = currentDate.withDayOfMonth(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 일요일을 0으로
    val daysInMonth = currentDate.lengthOfMonth()
    
    // 전체 셀 개수 계산 (6주 = 42개 셀)
    val totalCells = 42
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(6) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { dayOfWeek ->
                    val cellIndex = week * 7 + dayOfWeek
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // 이전 달 날짜들 (빈 공간)
                            cellIndex < firstDayOfWeek -> {
                                Spacer(modifier = Modifier.size(40.dp))
                            }
                            // 현재 달 날짜들
                            cellIndex < firstDayOfWeek + daysInMonth -> {
                                val day = cellIndex - firstDayOfWeek + 1
                                val isSelected = day == selectedDay
                                val isToday = day == currentDate.dayOfMonth
                                
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> CampusPrimary
                                                isToday -> CampusPrimary.copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDayClick(day) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = when {
                                            isSelected -> Color.White
                                            dayOfWeek == 0 -> Color.Red // 일요일
                                            dayOfWeek == 6 -> Color.Blue // 토요일
                                            else -> Color.Black
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            // 다음 달 날짜들 (빈 공간)
                            else -> {
                                Spacer(modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}