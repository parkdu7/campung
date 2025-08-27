package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

        // ✅ 날짜만 카드(내부 패딩만 적용)
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            onClick = onDateClick,                  // 클릭은 Card에만
            modifier = Modifier                     // ⛔ 바깥 padding 없음!
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 10.dp),  // ✨ 내부 패딩만
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(selectedDate),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Icon(
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = "날짜 선택",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
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
                        tint = Color.Gray
                    )
                }
                
                // 알림 점
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color.Red,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
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