package com.shinhan.campung.presentation.ui.screens.contentdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    contentId: Long,
    navController: NavController,
    viewModel: ContentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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

    ContentDetailScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onLikeClick = { viewModel.toggleLike(contentId) },
        onCommentTextChange = viewModel::updateCommentText,
        onSendComment = { viewModel.postComment(contentId, uiState.commentText) },
        onReplyClick = { commentId ->
            // TODO: 대댓글 작성 기능 구현
        }
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
    onReplyClick: (Long) -> Unit
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
                    // 우측 공간을 맞추기 위한 빈 공간
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
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
                // 컨텐츠 제목
                item {
                    TitleSection(title = content.title)
                }
                
                // 작성자 정보
                item {
                    AuthorSection(
                        author = content.author,
                        createdAt = content.createdAtDateTime
                    )
                }

                // 미디어 파일 (이미지/비디오)
                if (!content.mediaFiles.isNullOrEmpty()) {
                    item {
                        MediaPagerSection(mediaFiles = content.mediaFiles)
                    }
                }

                // 컨텐츠 텍스트
                item {
                    ContentTextSection(content = content.body)
                }

                // 좋아요/댓글 수 바
                item {
                    InteractionBar(
                        isLiked = uiState.isLiked,
                        likeCount = uiState.likeCount,
                        commentCount = uiState.commentCount,
                        onLikeClick = onLikeClick
                    )
                }

                // 댓글 목록
                items(uiState.comments) { comment ->
                    CommentItem(
                        comment = comment,
                        onReplyClick = onReplyClick
                    )
                }

                // 댓글 로딩 표시
                if (uiState.isCommentLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // 댓글 입력 바
            CommentInputBar(
                commentText = uiState.commentText,
                onCommentTextChange = onCommentTextChange,
                onSendComment = onSendComment,
                modifier = Modifier.padding(
                    WindowInsets.ime.exclude(WindowInsets.navigationBars).asPaddingValues()
                )
            )
        }
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