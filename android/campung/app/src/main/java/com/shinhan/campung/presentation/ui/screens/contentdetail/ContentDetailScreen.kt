package com.shinhan.campung.presentation.ui.screens.contentdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shinhan.campung.presentation.ui.theme.CampusBackground
import com.shinhan.campung.presentation.viewmodel.ContentDetailViewModel
import com.shinhan.campung.presentation.ui.components.comment.CommentInputBar
import com.shinhan.campung.presentation.ui.components.comment.CommentItem
import com.shinhan.campung.presentation.ui.components.content.AuthorSection
import com.shinhan.campung.presentation.ui.components.content.ContentTextSection
import com.shinhan.campung.presentation.ui.components.content.InteractionBar
import com.shinhan.campung.presentation.ui.components.content.MediaPagerSection
import com.shinhan.campung.presentation.ui.components.content.TitleSection
import com.shinhan.campung.presentation.ui.theme.CampusAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    contentId: Long,
    navController: NavController,
    viewModel: ContentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(contentId) {
        viewModel.loadContent(contentId)
        viewModel.loadComments(contentId)
    }

    // 에러 스낵바 표시
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // 스낵바 표시 후 에러 클리어
            viewModel.clearError()
        }
    }
    
    // 대댓글 모드 진입 시 키보드 포커스
    LaunchedEffect(uiState.selectedCommentId) {
        if (uiState.selectedCommentId != null) {
            focusRequester.requestFocus()
        }
    }

    ContentDetailScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onLikeClick = { viewModel.toggleLike(contentId) },
        onCommentTextChange = viewModel::updateCommentText,
        onSendComment = { 
            if (uiState.isReplyMode && uiState.selectedCommentId != null) {
                viewModel.postReply(uiState.selectedCommentId!!, uiState.commentText)
            } else {
                viewModel.postComment(contentId, uiState.commentText)
            }
        },
        onReplyClick = { commentId ->
            viewModel.selectCommentForReply(commentId)
        },
        onClearReplyMode = {
            viewModel.clearReplyMode()
        },
        onDeleteClick = {
            viewModel.showDeleteDialog()
        },
        onDeleteConfirm = {
            viewModel.deleteContent(contentId) { navController.popBackStack() }
        },
        onDeleteCancel = {
            viewModel.hideDeleteDialog()
        },
        focusRequester = focusRequester
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentDetailScreenContent(
    uiState: ContentDetailUiState,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onReplyClick: (Long) -> Unit,
    onClearReplyMode: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    onDeleteCancel: () -> Unit = {},
    focusRequester: FocusRequester = FocusRequester()
) {
    if (uiState.isLoading) {
        LoadingContent(onBackClick = onBackClick)
        return
    }

    val content = uiState.content
    if (content == null) {
        ErrorContent(
            onBackClick = onBackClick,
            error = uiState.error ?: "컨텐츠를 찾을 수 없습니다"
        )
        return
    }

    Scaffold(
        containerColor = CampusBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "${content.postTypeName}게시판",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    // 본인 게시글일 때만 더보기 메뉴 표시
                    if (uiState.isMyContent) {
                        var showMenu by remember { mutableStateOf(false) }
                        
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "더보기",
                                    tint = Color.Black
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "삭제",
                                            color = CampusAccent
                                        ) 
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    } else {
                        // 본인 게시글이 아닐 때는 빈 공간
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CampusBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 제목
                item {
                    Box(Modifier.fillMaxWidth().padding(start = 15.dp, top = 15.dp)) {
                        TitleSection(title = content.title)
                    }
                }

                // 작성자 정보 (프로필)
                item {
                    Box(Modifier.padding(start = 15.dp).fillMaxWidth()) {
                        AuthorSection(
                            author = content.author,
                            createdAt = content.createdAtDateTime
                        )
                    }
                }

                // 본문
                item {
                    Box(Modifier.fillMaxWidth().padding(start = 15.dp)) {
                        ContentTextSection(content = content.body ?: "")
                    }
                }

                // 미디어(이미지/동영상) — 들여쓰기 X (그대로)
                if (!content.mediaFiles.isNullOrEmpty()) {
                    item { MediaPagerSection(mediaFiles = content.mediaFiles) }
                }

                // 좋아요/댓글 수 바
                item {
                    Box(Modifier.fillMaxWidth().padding(start = 15.dp)) {
                        InteractionBar(
                            isLiked = uiState.isLiked,
                            likeCount = uiState.likeCount,
                            commentCount = uiState.commentCount,
                            onLikeClick = onLikeClick
                        )
                    }
                }

                // 댓글 영역 구분선 (풀블리드 유지 시 그대로)
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        thickness = 1.dp,
                        color = Color(0xFFE0E0E0)
                    )
                }

                // 빈 댓글 상태 문구
                if (uiState.comments.isEmpty() && !uiState.isCommentLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(start = 15.dp)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "첫 번째 댓글을 남겨보세요!",
                                    fontSize = 16.sp,
                                    color = Color(0xFF999999),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "이 게시물에 대한 생각을 공유해 주세요",
                                    fontSize = 14.sp,
                                    color = Color(0xFFBBBBBB)
                                )
                            }
                        }
                    }
                } else {
                    // 댓글 목록
                    items(uiState.comments.size) { index ->
                        val comment = uiState.comments[index]
                        Box(Modifier.fillMaxWidth().padding(start = 15.dp)) {
                            CommentItem(
                                comment = comment,
                                onReplyClick = onReplyClick,
                                isSelected = comment.commentId == uiState.selectedCommentId
                            )
                        }
                        if (index < uiState.comments.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFF0F0F0)
                            )
                        }
                    }
                }

                if (uiState.isCommentLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(Modifier.size(24.dp)) }
                    }
                }
            }

            // 댓글 입력 바
            CommentInputBar(
                commentText = uiState.commentText,
                onCommentTextChange = onCommentTextChange,
                onSendComment = onSendComment,
                isReplyMode = uiState.isReplyMode,
                selectedCommentAuthor = uiState.selectedCommentAuthor,
                onClearReplyMode = onClearReplyMode,
                focusRequester = focusRequester,
                modifier = Modifier.padding(
                    WindowInsets.ime.exclude(WindowInsets.navigationBars).asPaddingValues()
                )
            )
        }
    }
    
    // 삭제 확인 다이얼로그
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteCancel,
            title = { 
                Text(
                    "게시글 삭제",
                    color = Color.Black
                ) 
            },
            text = { 
                Text(
                    "정말로 이 게시글을 삭제하시겠습니까?",
                    color = Color.Black
                ) 
            },
            containerColor = Color.White,
            confirmButton = {
                if (uiState.isDeleting) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                } else {
                    TextButton(
                        onClick = onDeleteConfirm
                    ) {
                        Text("삭제", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteCancel,
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        "취소",
                        color = Color.Black
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingContent(onBackClick: () -> Unit) {
    Scaffold(
        containerColor = CampusBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "로딩 중...",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorContent(
    onBackClick: () -> Unit,
    error: String
) {
    Scaffold(
        containerColor = CampusBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "오류",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "네트워크 연결을 확인해 주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}