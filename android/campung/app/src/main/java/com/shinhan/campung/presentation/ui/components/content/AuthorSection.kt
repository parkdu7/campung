package com.shinhan.campung.presentation.ui.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shinhan.campung.data.model.Author
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun AuthorSection(
    author: Author,
    createdAt: LocalDateTime,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (author.isAnonymous) null else author.profileImageUrl,
            contentDescription = "프로필 이미지",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = if (author.isAnonymous) "익명" else author.nickname,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = formatTimeAgo(createdAt),
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

private fun formatTimeAgo(dateTime: LocalDateTime): String {
    // 한국 시간으로 변환
    val koreaZone = ZoneId.of("Asia/Seoul")
    val now = ZonedDateTime.now(koreaZone).toLocalDateTime()
    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    
    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        minutes < 1440 -> "${minutes / 60}시간 전"
        minutes < 10080 -> "${minutes / 1440}일 전"
        else -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"))
    }
}