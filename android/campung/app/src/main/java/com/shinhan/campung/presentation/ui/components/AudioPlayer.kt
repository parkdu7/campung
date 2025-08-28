package com.shinhan.campung.presentation.ui.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AudioPlayer(
    recordUrl: String,
    recordId: Long,
    authorName: String,
    createdAt: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // MediaPlayer 초기화 및 정리
    LaunchedEffect(recordUrl) {
        try {
            isLoading = true
            errorMessage = null
            
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recordUrl)
                setOnPreparedListener { mp ->
                    duration = mp.duration
                    isLoading = false
                    Log.d("AudioPlayer", "오디오 준비 완료: ${duration}ms")
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer 오류: what=$what, extra=$extra")
                    errorMessage = "오디오 재생 중 오류가 발생했습니다"
                    isLoading = false
                    isPlaying = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "MediaPlayer 초기화 실패", e)
            errorMessage = "오디오를 로드할 수 없습니다"
            isLoading = false
        }
    }

    // 진행률 업데이트
    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        currentPosition = mp.currentPosition
                    }
                }
                delay(100)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "진행률 업데이트 실패", e)
                break
            }
        }
    }

    // 컴포넌트 해제 시 MediaPlayer 정리
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "MediaPlayer 해제 실패", e)
            }
        }
    }

    // 고정 높이 컨테이너로 애니메이션 시 크기 변화 방지
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp) // 고정 높이 설정
            .padding(horizontal = 16.dp)
    ) {
        // 글래스모피즘 효과를 위한 배경
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.85f)
                        )
                    )
                )
        ) {
            // 메인 콘텐츠
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 상단 영역: 헤더와 닫기 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 음성 아이콘과 정보
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 음성 아이콘 배경
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color(0xFFFF6B6B).copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "음성",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = "음성 메모",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2D2D2D)
                            )
                            Text(
                                text = "$authorName • ${formatDate(createdAt)}",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    // 닫기 버튼
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.1f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // 하단 영역: 플레이어 컨트롤 (고정 높이 영역)
                Box(
                    modifier = Modifier.height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // 에러 상태
                        errorMessage != null -> {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF6B6B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // 로딩 상태
                        isLoading -> {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFFF6B6B)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "로딩 중...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // 재생 컨트롤
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 재생 컨트롤 바
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 재생/일시정지 버튼
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPlaying) Color(0xFFFF6B6B) else Color(0xFF4CAF50)
                                            )
                                            .clickable {
                                                try {
                                                    mediaPlayer?.let { mp ->
                                                        if (isPlaying) {
                                                            mp.pause()
                                                            isPlaying = false
                                                        } else {
                                                            mp.start()
                                                            isPlaying = true
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("AudioPlayer", "재생/일시정지 실패", e)
                                                    errorMessage = "재생 중 오류가 발생했습니다"
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "일시정지" else "재생",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    // 시간 정보
                                    Text(
                                        text = if (duration > 0) {
                                            "${formatTime(currentPosition)} / ${formatTime(duration)}"
                                        } else {
                                            "00:00 / 00:00"
                                        },
                                        fontSize = 13.sp,
                                        color = Color(0xFF666666),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 진행 바
                                val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFE0E0E0))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFFFF6B6B),
                                                        Color(0xFFFF8E8E)
                                                    )
                                                ),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("M월 d일 HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}