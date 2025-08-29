package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinhan.campung.R
import com.shinhan.campung.data.model.MapRecord
import com.shinhan.campung.presentation.utils.TimeFormatter

@Composable
fun MapRecordItem(
    record: MapRecord,
    isPlaying: Boolean = false,
    currentPlayingRecord: MapRecord? = null,
    modifier: Modifier = Modifier,
    onPlayClick: (MapRecord) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isCurrentPlaying = currentPlayingRecord?.recordId == record.recordId

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 스피커 아이콘과 플레이 버튼 영역 - 정사각형
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3E5F5)), // 연한 보라색 배경 (녹음 표시)
            ) {
                // 큰 스피커 아이콘을 중앙에 표시
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "오디오",
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = Color(0xFF9C27B0)
                )
            }
            
            // 녹음 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 녹음 타이틀과 아이콘
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "재생",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF9C27B0)
                    )
                    
                    Text(
                        text = "음성 녹음",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF9C27B0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 재생 상태 표시
                if (isCurrentPlaying) {
                    Text(
                        text = if (isPlaying) "재생 중..." else "일시정지됨",
                        fontSize = 14.sp,
                        color = Color(0xFF9C27B0),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "탭하여 재생",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                // Spacer로 하단에 메타정보 고정
                Spacer(modifier = Modifier.weight(1f))
                
                // 작성자 정보와 시간
                Text(
                    text = "${record.author.nickname} · ${TimeFormatter.formatRelativeTime(parseDateTime(record.createdAt))}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Record용 날짜 파싱 유틸리티 (MapRecordItem에서 사용)
private fun parseDateTime(dateStr: String): java.time.LocalDateTime {
    return try {
        val cleanDateStr = dateStr.removeSuffix("Z")
        val truncatedStr = if (cleanDateStr.contains(".") && cleanDateStr.substringAfter(".").length > 3) {
            val beforeDot = cleanDateStr.substringBefore(".")
            val afterDot = cleanDateStr.substringAfter(".")
            val truncatedMicros = afterDot.take(3)
            "$beforeDot.$truncatedMicros"
        } else {
            cleanDateStr
        }
        java.time.LocalDateTime.parse(truncatedStr)
    } catch (e: Exception) {
        // 한국 현재 시간으로 fallback
        java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime()
    }
}