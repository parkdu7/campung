package com.shinhan.campung.presentation.ui.components.content

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.shinhan.campung.data.model.MediaFile

@androidx.media3.common.util.UnstableApi
@Composable
fun MediaPagerSection(
    mediaFiles: List<MediaFile>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        pageCount = { mediaFiles.size }
    )
    var showFullScreenImage by remember { mutableStateOf(false) }
    var fullScreenImageUrl by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val mediaFile = mediaFiles[page]
            
            when (mediaFile.fileType) {
                "IMAGE" -> {
                    AsyncImage(
                        model = mediaFile.fileUrl,
                        contentDescription = "이미지",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                fullScreenImageUrl = mediaFile.fileUrl
                                showFullScreenImage = true
                            },
                        contentScale = ContentScale.Crop
                    )
                }
                "VIDEO" -> {
                    VideoPlayerView(
                        videoUrl = mediaFile.fileUrl,
                        isCurrentPage = pagerState.currentPage == page
                    )
                }
            }
        }

        // 페이지 인디케이터
        if (mediaFiles.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1}/${mediaFiles.size}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
    
    // 전체화면 이미지 다이얼로그
    if (showFullScreenImage) {
        FullScreenImageDialog(
            imageUrl = fullScreenImageUrl,
            onDismiss = { showFullScreenImage = false }
        )
    }
}

@Composable
private fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "전체화면 이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color.White
                )
            }
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
private fun VideoPlayerView(
    videoUrl: String,
    isCurrentPage: Boolean
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(true) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            volume = 0f // 기본 음소거
        }
    }

    DisposableEffect(isCurrentPage) {
        if (isCurrentPage) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
        
        onDispose {
            if (!isCurrentPage) {
                exoPlayer.pause()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 음소거 토글 버튼
        IconButton(
            onClick = {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "음소거 해제" else "음소거",
                tint = Color.White
            )
        }

        // 전체 화면 클릭으로 음소거 토글
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else 1f
                }
        )
    }
}