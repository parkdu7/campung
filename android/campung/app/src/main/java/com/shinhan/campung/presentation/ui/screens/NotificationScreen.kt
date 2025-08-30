package com.shinhan.campung.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shinhan.campung.presentation.viewmodel.NotificationViewModel

// 알림 데이터 클래스
data class NotificationItem(
    val id: Long,
    val type: String, // "normal", "friendRequest", "locationShareRequest"
    val title: String,
    val message: String,
    val data: String? = null, // JSON 형태의 추가 데이터
    val isRead: Boolean = false,
    val createdAt: String,
    val requesterId: String? = null // 요청자 ID (friendRequest, locationShareRequest용)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    // ViewModel 상태들
    val uiState by viewModel.uiState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    // 에러 처리
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // TODO: 토스트 메시지 또는 스낵바로 에러 표시
            viewModel.clearError()
        }
    }

    // 성공 메시지 처리
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            // TODO: 토스트 메시지 또는 스낵바로 성공 메시지 표시
            viewModel.clearSuccessMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 상단 앱바
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "알림",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )

        // 로딩 상태 표시
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF788CF7))
            }
        }

        // 처리 완료된 알림들을 제외하고 표시
        val displayNotifications = notifications.filter { notification ->
            notification.type !in listOf("location_share_accepted", "friend_request_accepted")
        }
        
        // 알림 목록 또는 빈 상태 표시
        if (displayNotifications.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "알림이 없습니다",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(displayNotifications) { notification ->
                    NotificationListItem(
                        notification = notification,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onAcceptClick = { notificationId ->
                            when (notification.type) {
                                "friend_request", "friendRequest" -> {
                                    // 친구 요청 수락
                                    viewModel.acceptFriendRequest(notificationId)
                                }
                                "location_share_request" -> {
                                    // 위치 공유 요청 수락
                                    viewModel.acceptLocationShareRequest(notificationId)
                                }
                            }
                        },
                        onRejectClick = { notificationId ->
                            when (notification.type) {
                                "friend_request", "friendRequest" -> {
                                    // 친구 요청 거절
                                    viewModel.rejectFriendRequest(notificationId)
                                }
                                "location_share_request" -> {
                                    // 위치 공유 요청 거절
                                    viewModel.rejectLocationShareRequest(notificationId)
                                }
                            }
                        },
                        onNotificationClick = { notificationId ->
                            // 게시글 관련 알림 클릭 처리
                            when (notification.type) {
                                "post_like", "post_comment" -> {
                                    // data 필드에서 contentId 추출
                                    notification.data?.let { data ->
                                        try {
                                            // JSON 파싱하여 contentId 추출 (간단한 방법)
                                            val contentId = extractContentIdFromData(data)
                                            contentId?.let {
                                                // TODO: Navigation을 통해 ContentDetailScreen으로 이동
                                                // 이 부분은 MainActivity에서 처리하도록 수정 필요
                                            }
                                        } catch (e: Exception) {
                                            // 파싱 실패 시 무시
                                        }
                                    }
                                }
                            }
                        }
                    )
                    Divider(
                        color = Color.Gray.copy(alpha = 0.2f),
                        thickness = 0.5.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(
    notification: NotificationItem,
    modifier: Modifier = Modifier,
    onAcceptClick: (Long) -> Unit,
    onRejectClick: (Long) -> Unit,
    onNotificationClick: (Long) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(
                // 게시글 관련 알림은 클릭 가능하게 만들기
                if (notification.type in listOf("post_like", "post_comment")) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onNotificationClick(notification.id)
                    }
                } else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 알림 내용
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 타입별 메시지 표시
            when (notification.type) {
                "normal", "post_like", "post_comment" -> {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                "friend_request", "friendRequest" -> {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                "location_share_request" -> {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // 타입별 버튼 표시
        when (notification.type) {
            "normal", "post_like", "post_comment", "location_share_accepted" -> {
                // 일반 알림과 게시글 관련 알림은 버튼 없음 (클릭으로 처리)
            }
            "friend_request", "friendRequest", "location_share_request" -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 거절 버튼
                    Button(
                        onClick = { onRejectClick(notification.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF3F4F6)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "거절",
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                    }

                    // 수락 버튼
                    Button(
                        onClick = { onAcceptClick(notification.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF788CF7)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "수락",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// 데이터 클래스들
data class NotificationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

// 헬퍼 함수
private fun extractContentIdFromData(data: String): Long? {
    return try {
        // 간단한 JSON 파싱 (contentId만 추출)
        val regex = "\"contentId\"\\s*:\\s*(\\d+)".toRegex()
        val matchResult = regex.find(data)
        matchResult?.groupValues?.get(1)?.toLongOrNull()
    } catch (e: Exception) {
        null
    }
}

