package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.res.painterResource
import com.shinhan.campung.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTopHeader(
    selectedDate: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit,
    onDateClick: () -> Unit,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onFriendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 뒤로가기 버튼
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.Gray
            )
        }

        // 날짜 선택기 (좌우 화살표 포함)
        Card(
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 이전 날짜 화살표
                IconButton(
                    onClick = onPreviousDate,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.calendar_chevron_left),
                        contentDescription = "이전 날짜",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // 날짜 텍스트 (클릭 가능)
                Row(
                    modifier = Modifier.clickable { onDateClick() },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatKoreanDate(selectedDate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.calendar_down_arrow),
                        contentDescription = "날짜 선택",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(8.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                }
                
                // 다음 날짜 화살표
                IconButton(
                    onClick = onNextDate,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.calendar_chevron_right),
                        contentDescription = "다음 날짜",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }


        // 친구 버튼 (빨간 알림 점 포함)
        Box {
            IconButton(
                onClick = onFriendClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.friend_icon),
                    contentDescription = "친구",
                    tint = Color.Unspecified,          // ← 원본 색 유지!
                    modifier = Modifier.fillMaxSize()  // 필요 시 아이콘 채우기
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red, shape = CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
            )
        }

    }
}

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN)
    return date.format(formatter)
}

private fun formatKoreanDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd", Locale.KOREAN)
    return date.format(formatter) + "."
}