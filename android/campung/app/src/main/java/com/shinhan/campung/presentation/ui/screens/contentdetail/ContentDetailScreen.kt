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
                
                // 작성자 정보 구분선
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFE0E0E0)
                    )
                }

                // 미디어 파일 (이미지/비디오)
                if (!content.mediaFiles.isNullOrEmpty()) {
                    item {
                        MediaPagerSection(mediaFiles = content.mediaFiles)
                    }
                    
                    // 미디어 구분선
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }

                // 컨텐츠 텍스트
                item {
                    ContentTextSection(content = content.body)
                }
                
                // 컨텐츠 텍스트 구분선
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFE0E0E0)
                    )
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
                
                // 댓글 영역 구분선
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        thickness = 1.dp,
                        color = Color(0xFFE0E0E0)
                    )
                }

                // 댓글 목록 또는 빈 상태
                if (uiState.comments.isEmpty() && !uiState.isCommentLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
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
                        
                        CommentItem(
                            comment = comment,
                            onReplyClick = onReplyClick,
                            isSelected = comment.commentId == uiState.selectedCommentId
                        )
                        
                        // 댓글 사이 구분선 (마지막 댓글은 제외)
                        if (index < uiState.comments.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFF0F0F0)
                            )
                        }
                    }
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