package com.shinhan.campung.presentation.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.shinhan.campung.presentation.viewmodel.WritePostViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePostScreen(
    onBack: () -> Unit = {},
    onSubmitted: (Long) -> Unit = {}
) {
    val viewModel: WritePostViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    // 비디오 프레임 디코더 등록된 ImageLoader
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- 로컬 상태 ---
    val boards = remember {
        listOf(
            BoardItem("장터게시판", "전공책 등 중고물품을 사고 팔 수 있어요."),
            BoardItem("홍보게시판", "동아리, 학회, 스터디 등을 홍보할 수 있어요."),
            BoardItem("자유게시판", "헤이영 친구들과 자유롭게 소통하고 즐겨요."),
            BoardItem("정보게시판", "친구들에게 꼭 필요한 정보를 공유해 주세요.")
        )
    }
    val accent = Color(0xFF188BAA)
    val bg = Color(0xFFF6F6F6)
    val borderColor = Color(0xFFD9D9D9)
    val fieldShape = RoundedCornerShape(14.dp)

    var showBoardSheet by rememberSaveable { mutableStateOf(true) }
    var selectedBoardTitle by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedBoard = remember(boards, selectedBoardTitle) {
        boards.firstOrNull { it.title == selectedBoardTitle }
    }

    var isRealName by rememberSaveable { mutableStateOf(false) } // true=실명, false=익명
    val displayName = "박승균"
    var nickname by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }

    // 사진/영상 공통 URI (최대 3)
    val mediaUris = remember { mutableStateListOf<Uri>() }

    // 갤러리(사진+영상)
    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remain = 3 - mediaUris.size
            if (remain > 0) mediaUris.addAll(uris.take(remain))
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 이벤트 수집
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WritePostViewModel.Event.Success -> {
                    scope.launch { snackbarHostState.showSnackbar("등록 완료! #${event.contentId}") }
                    onSubmitted(event.contentId)
                }
                is WritePostViewModel.Event.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    // --- UI ---
    CompositionLocalProvider(LocalImageLoader provides imageLoader) {
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .systemBarsPadding()
        ) {
            Column(Modifier.fillMaxSize()) {
                // 상단 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }

                // 폼
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // 게시판 선택
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(fieldShape)
                            .border(1.dp, borderColor, fieldShape)
                            .background(Color.White)
                            .clickable { showBoardSheet = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedBoard?.title ?: "게시판을 선택해 주세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedBoard == null) Color(0xFF6F6F6F) else Color(
                                0xFF111111
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                    }

                    // 실명/익명 토글
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { isRealName = !isRealName }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (!isRealName) accent else Color(0xFFBDBDBD)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(text = if (isRealName) "실명게시" else "익명게시")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 닉네임(익명) 입력 - 서버 스펙엔 없지만 UI 보존
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        placeholder = {
                            Text(text = if (!isRealName) displayName else "익명")
                        },
                        singleLine = true,
                        shape = fieldShape,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = borderColor,
                            disabledBorderColor = borderColor
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // 제목
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("제목을 입력해주세요.") },
                        singleLine = true,
                        shape = fieldShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = borderColor,
                            disabledBorderColor = borderColor
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    // 내용 + 툴바
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 400.dp)
                            .clip(fieldShape)
                            .border(1.dp, borderColor, fieldShape)
                            .background(Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (content.isEmpty()) {
                                Text("내용을 입력해주세요.", color = Color(0xFF9E9E9E))
                            }
                            BasicTextField(
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                            )
                        }

                        Divider(color = borderColor)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val remain = 3 - mediaUris.size
                                    if (remain > 0) {
                                        mediaPicker.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                            )
                                        )
                                    }
                                },
                                enabled = mediaUris.size < 3 && !uiState.isLoading
                            ) {
                                Icon(Icons.Outlined.AddCircle, contentDescription = "미디어 추가")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${mediaUris.size}/3")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 미디어 미리보기
                    if (mediaUris.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            mediaUris.forEachIndexed { idx, uri ->
                                MediaThumb(
                                    uri = uri,
                                    onRemove = { mediaUris.removeAt(idx) },
                                    borderColor = borderColor
                                )
                            }
                        }
                    }
                }

                // 등록 버튼
                val canSubmit = selectedBoard != null && title.isNotBlank() && content.isNotBlank()
                Button(
                    onClick = {
                        val fileUris = mediaUris.toList()
                        viewModel.submit(
                            boardTitle = selectedBoardTitle,
                            title = title,
                            body = content,
                            isRealName = isRealName,
                            emotionTag = null,
                            files = fileUris,
                            latitude = null,
                            longitude = null
                        )
                    },
                    enabled = canSubmit && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (uiState.isLoading) "등록 중..." else "등록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 스낵바
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(hostState = snackbarHostState)
            }

            // 로딩 오버레이
            if (uiState.isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 게시판 선택 바텀시트
            if (showBoardSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBoardSheet = false },
                    sheetState = sheetState,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "게시판을 선택해 주세요.",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        Divider()

                        Spacer(Modifier.height(2.dp))
                        boards.forEachIndexed { idx, item ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        item.title,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                supportingContent = { Text(item.description, color = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBoardTitle = item.title
                                        showBoardSheet = false
                                    }
                                    .padding(vertical = 6.dp)
                            )
                            if (idx != boards.lastIndex) Divider()
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

data class BoardItem(val title: String, val description: String)

@Composable
fun MediaThumb(
    uri: Uri,
    onRemove: () -> Unit,
    borderColor: Color
) {
    val context = LocalContext.current
    val mime by remember(uri) {
        mutableStateOf(context.contentResolver.getType(uri) ?: "")
    }
    val isVideo = mime.startsWith("video")

    val request = remember(uri, isVideo) {
        ImageRequest.Builder(context)
            .data(uri)
            .apply { if (isVideo) videoFrameMillis(1000) } // ✅ 1초 프레임
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 삭제 버튼
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, borderColor, CircleShape)
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "삭제", tint = Color.Black)
        }

        // 영상이면 ▶︎ 오버레이
        if (isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .background(Color(0x66000000), CircleShape)
                    .padding(2.dp)
            )
        }
    }
}
