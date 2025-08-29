package com.shinhan.campung.presentation.ui.components.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

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
            // ✅ 포커스/비포커스 시각 동일하게 (보더/컨테이너/텍스트/플레이스홀더/커서)
            val tfColors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,   // 밑줄 제거
                unfocusedIndicatorColor = Color.Transparent, // 밑줄 제거
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedPlaceholderColor = Color(0xFFBDBDBD),
                unfocusedPlaceholderColor = Color(0xFFBDBDBD),
                cursorColor = Color.Black
            )

            val shape = RoundedCornerShape(12.dp)

            TextField(
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
                    .focusRequester(focusRequester)
                    .border(1.dp, Color(0xFFE0E0E0), shape) // ✅ 항상 동일한 보더
                    .clip(shape),
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { if (commentText.isNotBlank()) onSendComment() }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal // ✅ 굵기 고정
                ),
                colors = tfColors,
                shape = shape
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendComment,
                enabled = commentText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "댓글 전송",
                    tint = if (commentText.isNotBlank()) Color.Gray else LocalContentColor.current
                )
            }
        }
    }
}
