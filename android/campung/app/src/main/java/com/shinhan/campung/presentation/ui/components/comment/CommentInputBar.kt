package com.shinhan.campung.presentation.ui.components.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInputBar(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    modifier: Modifier = Modifier,
    isReplyMode: Boolean = false,
    selectedCommentAuthor: String? = null,
    onClearReplyMode: () -> Unit = {},
    focusRequester: FocusRequester = FocusRequester()
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 대댓글 모드 표시
        if (isReplyMode && selectedCommentAuthor != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@${selectedCommentAuthor}님에게 답글 작성 중",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClearReplyMode,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "대댓글 모드 취소",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                placeholder = { 
                    Text(
                        if (isReplyMode && selectedCommentAuthor != null) 
                            "@${selectedCommentAuthor}님에게 답글을 입력하세요..."
                        else
                            "댓글을 입력하세요..."
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { if (commentText.isNotBlank()) onSendComment() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
        
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onSendComment,
                enabled = commentText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "댓글 전송",
                    tint = if (commentText.isNotBlank()) LocalContentColor.current else Color.Gray
                )
            }
        }
    }
}