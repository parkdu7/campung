package com.shinhan.campung.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.onFocusChanged
import androidx.hilt.navigation.compose.hiltViewModel
import com.shinhan.campung.R
import com.shinhan.campung.presentation.viewmodel.FriendViewModel

// 친구 데이터 클래스 (UI용)
data class Friend(
    val name: String,
    val email: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    onBackClick: () -> Unit = {},
    onAddFriendClick: () -> Unit = {},
    viewModel: FriendViewModel = hiltViewModel()
) {
    // ViewModel 상태들
    val uiState by viewModel.uiState.collectAsState()
    val friends by viewModel.friendsList.collectAsState()

    // 다이얼로그 상태
    var showAddFriendDialog by remember { mutableStateOf(false) }

    // 검색 상태
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    // 검색된 친구 목록 (FriendResponse를 Friend로 변환)
    val filteredFriends = remember(searchQuery, friends) {
        val friendsList = friends.map { Friend(it.nickname, it.userId) }
        if (searchQuery.isEmpty()) {
            friendsList
        } else {
            friendsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

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
        // 상단 앱바 (항상 검색바 표시)
        TopAppBar(
            title = {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (!isSearchFocused && searchQuery.isEmpty()) {
                        {
                            Text(
                                text = "이름으로 친구를 검색하세요.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    textStyle = TextStyle(fontSize = 14.sp),
                    shape = RoundedCornerShape(20.dp),

                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isSearchFocused = focusState.isFocused
                        },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색",
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "검색어 지우기",
                                    tint = Color.Gray
                                )
                            }
                        }
                    } else null
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.Black
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.loadFriendsList() },
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = Color.Black
                    )
                }
                IconButton(
                    onClick = { showAddFriendDialog = true },
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.friend_plus),
                        contentDescription = "친구 추가",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // 친구 목록과 로딩 오버레이를 Box로 묶어서 위치 고정
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 친구 목록
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredFriends) { friend ->
                    FriendListItem(
                        friend = friend,
                        onInviteClick = {
                            // 위치공유요청 버튼 클릭 처리
                            viewModel.sendLocationShareRequest(friend.email) // email이 userId
                        }
                    )
                    Divider(
                        color = Color.Gray.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )
                }
            }
            
            // 로딩 상태를 오버레이로 표시 (리스트 위에 겹침)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF788CF7))
                }
            }
        }
    }

    // 친구 추가 다이얼로그
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onConfirm = { targetUserId ->
                viewModel.sendFriendRequest(targetUserId)
                showAddFriendDialog = false
            }
        )
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var userId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "추가할 친구의 아이디를\n입력해주세요.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = Color.Black,
                lineHeight = 24.sp
            )
        },
        text = {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    placeholder = { },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF788CF7),
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(userId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF788CF7)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Text(
                    text = "확인",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF3F4F6)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Text(
                    text = "취소",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    )
}

@Composable
fun FriendListItem(
    friend: Friend,
    onInviteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 친구 정보
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = friend.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = friend.email,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // 위치공유요청 버튼
        Button(
            onClick = onInviteClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF788CF7)
            ),
            shape = RoundedCornerShape(3.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "위치공유요청",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

